package com.fuchs.aktsParser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Service
public class AktService {

    private final AktRepository aktRepository;

    // Словник для мапінгу українських місяців
    private static final Map<String, Integer> UKR_MONTHS = Map.ofEntries(
            Map.entry("січня", 1), Map.entry("лютого", 2), Map.entry("березня", 3),
            Map.entry("квітня", 4), Map.entry("травня", 5), Map.entry("червня", 6),
            Map.entry("липня", 7), Map.entry("серпня", 8), Map.entry("вересня", 9),
            Map.entry("жовтня", 10), Map.entry("листопада", 11), Map.entry("грудня", 12)
    );

    public AktService(AktRepository aktRepository) {
        this.aktRepository = aktRepository;
    }

    @Transactional
    public void saveAkt(AktDto dto) {

        AktEntity actEntity = new AktEntity();
        actEntity.setNumberDoc(normalizeNumber(dto.getNumberDoc()));
        actEntity.setTypeDoc(dto.getTypeDoc());
        actEntity.setNameSupplier(dto.getNameSupplier());
        actEntity.setCodeSupplier(normalizeCodeSupplierOrIpnSupplier(dto.getCodeSupplier()));
        actEntity.setIpnSupplier(normalizeCodeSupplierOrIpnSupplier(dto.getIpnSupplier()));
        actEntity.setIncludeTax(dto.getIncludeTax());

        // 1. Нормалізація дати
        LocalDate parsedDate = normalizeDate(dto.getDateDoc());
        if (parsedDate != null) {
            actEntity.setDateDoc(parsedDate.toString()); // Збереже у форматі YYYY-MM-DD
        } else {
            actEntity.setDateDoc(dto.getDateDoc()); // Збереже як є, якщо парсинг не вдався
        }

        // 2. Очищення грошових сум
        actEntity.setAmountDoc(parseBigDecimal(dto.getAmountDoc()));
        actEntity.setTaxDoc(parseBigDecimal(dto.getTaxDoc()));
        actEntity.setFileName(dto.getFileName());

        // 3. Обробка позицій акту (items)
        if (dto.getItems() != null) {
            for (AktItemDto itemDto : dto.getItems()) {
                AktItemEntity itemEntity = new AktItemEntity();
                itemEntity.setArticul(itemDto.getArticul());
                itemEntity.setDescription(itemDto.getDescription());
                itemEntity.setUnits(itemDto.getUnits());

                itemEntity.setQuantity(parseQuantity(itemDto.getQuantity()));
                itemEntity.setPrice(parseBigDecimal(itemDto.getPrice()));
                itemEntity.setAmount(parseBigDecimal(itemDto.getAmount()));
                itemEntity.setTax(parseBigDecimal(itemDto.getTax()));

                actEntity.addItem(itemEntity);
            }
        }

        if (isDuplicate(actEntity)) {
            // Тут можна кинути кастомний RuntimeException,
            // який контролер перехопить і поверне 409 Conflict
            throw new DuplicateDocumentException("Документ з такими даними вже існує!");
        }

        aktRepository.save(actEntity);
    }

    private LocalDate normalizeDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        // Очищення рядка від " р.", " р", зайвих пробілів та переведення в нижній регістр
        String cleanDate = rawDate.trim()
                .toLowerCase()
                .replaceAll("р\\.?\\s*$", "")
                .trim();

        // Перевірка числового формату (напр., "16.04.2026", "6.4.2026")
        if (cleanDate.matches("^\\d{1,2}\\.\\d{1,2}\\.\\d{4}$")) {
            try {
                return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("d.M.yyyy"));
            } catch (DateTimeParseException e) {
                return null;
            }
        }

        // Перевірка текстового формату (напр., "11 лютого 2026")
        String[] parts = cleanDate.split("\\s+");
        if (parts.length == 3) {
            try {
                int day = Integer.parseInt(parts[0]);
                Integer month = UKR_MONTHS.get(parts[1]);
                int year = Integer.parseInt(parts[2]);

                if (month != null) {
                    return LocalDate.of(year, month, day);
                }
            } catch (NumberFormatException | java.time.DateTimeException e) {
                return null;
            }
        }

        return null;
    }

    private BigDecimal parseBigDecimal(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }

        // 1. Очищення від розділювачів тисяч та "шуму" OCR
        String cleanAmount = rawAmount
                .replace(" ", "")       // Звичайний пробіл ("50 000,00" -> "50000,00")
                .replace("\u00a0", "")  // Нерозривний пробіл (часто буває в PDF)
                .replace("'", "")       // Апостроф ("5'200.00" -> "5200.00")
                .replace("\\", "")      // Зворотний слеш (помилка OCR "4\320.00" -> "4320.00")
                .replace("`", "");      // Зворотний апостроф (на всякий випадок)

        // 2. Заміна десяткової коми на крапку
        cleanAmount = cleanAmount.replace(",", "."); // "28000,00" -> "28000.00"

        try {
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            // Тут в ідеалі додати логування (напр. log.error("Не вдалося розпарсити суму: {}", rawAmount))
            // Щоб у консолі було видно, якщо OCR видав зовсім нечитабельний текст (напр. "500O.OO")
            return null;
        }
    }

    private String normalizeCodeSupplierOrIpnSupplier(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return null;
        }

        // Видаляємо всі символи, окрім цифр (коми, крапки, пробіли, букви тощо)
        String cleanCode = rawCode.replaceAll("\\D+", "");

        // Якщо після очищення рядок став пустим (напр., OCR розпізнав лише "-")
        if (cleanCode.isEmpty()) {
            return null;
        }

        return cleanCode;
    }

    private Integer parseQuantity(String rawQuantity) {
        if (rawQuantity == null || rawQuantity.isBlank()) {
            return null;
        }

        // 1. Прибираємо всі пробіли (на випадок "1 000")
        String cleanQty = rawQuantity.replaceAll("\\s+", "");

        // 2. Якщо є і крапка, і кома (напр. "1,000.00"),
        // або просто "сміття" — краще парсити це через логіку грошей.
        // Але для "quantity" найпростіший шлях — замінити кому на крапку
        // і розпарсити як Double.
        cleanQty = cleanQty.replace(",", ".");

        try {
            // "3.000" -> 3.0 -> int 3
            // "2.00"  -> 2.0 -> int 2
            // "5"     -> 5.0 -> int 5
            return (int) Math.round(Double.parseDouble(cleanQty));

        } catch (NumberFormatException e) {
            // Логування помилки
            return null;
        }
    }

    @Transactional(readOnly = true)
    public boolean isDuplicate(AktEntity aktEntity) {
        Optional<AktEntity> duplicate = Optional.empty();

        if (aktEntity.getCodeSupplier() != null && !aktEntity.getCodeSupplier().isBlank()) {
            // Шукаємо за ЄДРПОУ
            duplicate = aktRepository.findByDateDocAndCodeSupplierAndNumberDoc(
                    aktEntity.getDateDoc(), aktEntity.getCodeSupplier(), aktEntity.getNumberDoc());
        } else if (aktEntity.getIpnSupplier() != null && !aktEntity.getIpnSupplier().isBlank()) {
            // Шукаємо за ІПН, якщо коду немає
            duplicate = aktRepository.findByDateDocAndIpnSupplierAndNumberDoc(
                    aktEntity.getDateDoc(), aktEntity.getIpnSupplier(), aktEntity.getNumberDoc());
        }

        return duplicate.isPresent();
    }

    private String normalizeNumber(String rawNumber) {
        if (rawNumber == null || rawNumber.isBlank()) {
            return null;
        }
        // Видаляємо все, що не є цифрою (букви, пробіли, коми, крапки, символи "від" тощо)
        return rawNumber.replaceAll("\\D+", "");
    }
}
