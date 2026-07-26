package com.fuchs.nakladniParser;

import com.fuchs.aktsParser.AktEntity;
import com.fuchs.aktsParser.AktItemEntity;
import com.fuchs.aktsParser.AktRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Service
public class NakladnaProcessingService {

    // ВИКОРИСТОВУЄМО РЕПОЗИТОРІЙ АКТІВ!
    private final AktRepository aktRepository;

    private static final Map<String, Integer> UKR_MONTHS = Map.ofEntries(
            Map.entry("січня", 1), Map.entry("лютого", 2), Map.entry("березня", 3),
            Map.entry("квітня", 4), Map.entry("травня", 5), Map.entry("червня", 6),
            Map.entry("липня", 7), Map.entry("серпня", 8), Map.entry("вересня", 9),
            Map.entry("жовтня", 10), Map.entry("листопада", 11), Map.entry("грудня", 12)
    );

    public NakladnaProcessingService(AktRepository aktRepository) {
        this.aktRepository = aktRepository;
    }

    /**
     * @return true, якщо існуючий документ був оновлений (upsert), false — якщо створено новий
     */
    @Transactional
    public boolean processAndSaveNakladna(NakladnaDto dto) {
        // СТВОРЮЄМО СУТНІСТЬ АКТУ
        AktEntity aktEntity = new AktEntity();

        // Мапимо поля Накладної -> на поля Акту
        aktEntity.setTypeDoc(dto.getDocumentType() != null ? dto.getDocumentType().replace("\n", " ").trim() : "Накладна");
        aktEntity.setNumberDoc(normalizeNumber(dto.getInvoiceId()));
        aktEntity.setNameSupplier(dto.getVendorName() != null ? dto.getVendorName().replace("\n", " ").trim() : null);
        aktEntity.setIncludeTax(dto.getIncludeTax());

        aktEntity.setCodeSupplier(normalizeVendorCode(dto.getVendorEdrpou()));
        aktEntity.setIpnSupplier(normalizeVendorCode(dto.getVendorIpn()));

        // 1. Нормалізація дати
        LocalDate parsedDate = normalizeDate(dto.getInvoiceDate());
        if (parsedDate != null) {
            aktEntity.setDateDoc(parsedDate.toString()); // Збереже розпарсену дату у форматі YYYY-MM-DD
        } else {
            // Якщо дата невалідна або OCR видав повне сміття — сетаємо поточну дату
            aktEntity.setDateDoc(LocalDate.now().toString());
        }

        // Парсинг сум з вирішеною проблемою "грн."
        aktEntity.setAmountDoc(parseBigDecimal(dto.getTotalAmount()));
        aktEntity.setTaxDoc(parseBigDecimal(dto.getInvoiceTotalTax()));
        aktEntity.setFileName(dto.getFileName());
        aktEntity.setUploadedTo1C(false);

        // Обробка позицій (мапимо на AktItemEntity)
        if (dto.getProducts() != null) {
            dto.getProducts().forEach(productDto -> {
                AktItemEntity item = new AktItemEntity();
                item.setDescription(productDto.getDescription() != null ? productDto.getDescription().replace("\n", " ").trim() : null);

                // Оскільки Jackson вже конвертує quantity/amount у BigDecimal (якщо вони приходять числами)
                item.setQuantity(productDto.getQuantity().intValue());
                item.setAmount(productDto.getAmount());

                item.setUnits(productDto.getUnit() != null ? productDto.getUnit().replace("|", "").trim() : null);

                // Ціна часто приходить як рядок "14,52"
                item.setPrice(parseBigDecimal(productDto.getUnitPrice()));

                aktEntity.addItem(item);
            });
        }

        // Перевірка на дублікати в таблиці актів
        Optional<AktEntity> existing = findDuplicate(aktEntity);
        if (existing.isPresent()) {
            // Документ з такими ключами вже є в БД — оновлюємо його новими даними
            // замість того, щоб відхиляти завантаження
            AktEntity target = existing.get();
            updateEntity(target, aktEntity);
            aktRepository.save(target);
            return true;
        }

        // Зберігаємо в загальну таблицю
        aktRepository.save(aktEntity);
        return false;
    }

    /**
     * Повністю переносить дані з щойно розпарсеної накладної (source) в існуючий запис (target).
     */
    private void updateEntity(AktEntity target, AktEntity source) {
        target.setNumberDoc(source.getNumberDoc());
        target.setTypeDoc(source.getTypeDoc());
        target.setNameSupplier(source.getNameSupplier());
        target.setCodeSupplier(source.getCodeSupplier());
        target.setIpnSupplier(source.getIpnSupplier());
        target.setIncludeTax(source.getIncludeTax());
        target.setDateDoc(source.getDateDoc());
        target.setAmountDoc(source.getAmountDoc());
        target.setTaxDoc(source.getTaxDoc());
        target.setFileName(source.getFileName());
        target.setUploadedTo1C(false); // дані змінились — потрібен повторний вигруз в 1С

        target.getItems().clear();
        for (AktItemEntity item : source.getItems()) {
            target.addItem(item);
        }
    }

    // --- НАДІЙНИЙ ПАРСИНГ СУМ (ФІКС ПРОБЛЕМИ З ГРН) ---
    private BigDecimal parseBigDecimal(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }

        // 1. Очищення від розділювачів тисяч та артефактів OCR
        String cleanAmount = rawAmount
                .replace(" ", "")
                .replace("\u00a0", "")
                .replace("'", "")
                .replace("\\", "")
                .replace("`", "");

        // 2. Заміна десяткової коми на крапку
        cleanAmount = cleanAmount.replace(",", ".");

        // 3. Видаляємо всі літери (грн, uah, літери з назв тощо)
        cleanAmount = cleanAmount.replaceAll("[а-яА-Яa-zA-ZіІїЇєЄґҐ]+", "");

        // 4. Якщо в кінці залишилася крапка від скорочення "грн.", видаляємо її
        cleanAmount = cleanAmount.replaceAll("\\.+$", "");

        if (cleanAmount.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(cleanAmount).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeNumber(String rawNumber) {
        if (rawNumber == null || rawNumber.isBlank()) {
            return "";
        }
        // Залишаємо букви, цифри, дефіси та косі риски (щоб не втратити префікси Рнк/LV)
        return rawNumber.replaceAll("(?i)(№|від|номер|акт|документ|накладна|\\s)", "").trim();
    }

    private String normalizeVendorCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) return null;
        // Відрізає приставку "ИНН:" і залишає лише цифри
        String cleanCode = rawCode.replaceAll("\\D+", "");
        return cleanCode.isEmpty() ? null : cleanCode;
    }

    private LocalDate normalizeDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) return null;

        // 1. Замінюємо нижні підкреслення на пробіли (OCR часто так розпізнає зсуви тексту)
        // 2. Видаляємо всі можливі лапки (включно з подвійними " та одинарними ')
        // 3. Переводимо в нижній регістр і відрізаємо "р." в кінці
        String cleanDate = rawDate.trim()
                .replace("_", " ")
                .replaceAll("[«»\"'“”]", "")
                .toLowerCase()
                .replaceAll("р\\.?\\s*$", "")
                .trim();

        if (cleanDate.matches("^\\d{1,2}\\.\\d{1,2}\\.\\d{4}$")) {
            try { return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("d.M.yyyy")); }
            catch (DateTimeParseException e) { return null; }
        }

        // Для текстових дат кілька пробілів підряд (напр. "16    червня")
        // безпечно зіжмуться в один завдяки \\s+
        String[] parts = cleanDate.split("\\s+");
        if (parts.length == 3) {
            try {
                int day = Integer.parseInt(parts[0]);
                Integer month = UKR_MONTHS.get(parts[1]);
                int year = Integer.parseInt(parts[2]);
                if (month != null) return LocalDate.of(year, month, day);
            } catch (NumberFormatException | java.time.DateTimeException e) {
                return null;
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public Optional<AktEntity> findDuplicate(AktEntity aktEntity) {
        String num = aktEntity.getNumberDoc() != null ? aktEntity.getNumberDoc() : "";

        if (aktEntity.getCodeSupplier() != null && !aktEntity.getCodeSupplier().isBlank()) {
            return aktRepository.findByDateDocAndCodeSupplierAndNumberDoc(
                    aktEntity.getDateDoc(), aktEntity.getCodeSupplier(), num);
        } else if (aktEntity.getIpnSupplier() != null && !aktEntity.getIpnSupplier().isBlank()) {
            return aktRepository.findByDateDocAndIpnSupplierAndNumberDoc(
                    aktEntity.getDateDoc(), aktEntity.getIpnSupplier(), num);
        } else {
            // ФОЛБЕК: Якщо обох кодів немає, перевіряємо за назвою, датою та номером
            // Запобігає дублюванню документів без розпізнаних кодів компанії
            return aktRepository.findByDateDocAndNumberDocAndNameSupplier(
                    aktEntity.getDateDoc(), num, aktEntity.getNameSupplier());
        }
    }

}