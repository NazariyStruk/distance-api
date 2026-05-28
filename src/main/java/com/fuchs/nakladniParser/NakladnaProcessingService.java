package com.fuchs.nakladniParser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NakladnaProcessingService {

    private final NakladnaRepository nakladnaRepository;

    private static final Map<String, Integer> UKR_MONTHS = Map.ofEntries(
            Map.entry("січня", 1), Map.entry("лютого", 2), Map.entry("березня", 3),
            Map.entry("квітня", 4), Map.entry("травня", 5), Map.entry("червня", 6),
            Map.entry("липня", 7), Map.entry("серпня", 8), Map.entry("вересня", 9),
            Map.entry("жовтня", 10), Map.entry("листопада", 11), Map.entry("грудня", 12)
    );

    @Transactional
    public void processAndSaveNakladna(NakladnaDto dto) {
        NakladnaEntity nakladna = new NakladnaEntity();

        nakladna.setDocumentType(dto.getDocumentType() != null ? dto.getDocumentType().replace("\n", " ").trim() : null);

        // Зберігаємо номер як є (щоб не втратити префікси типу Рнк/LV)
        nakladna.setInvoiceId(dto.getInvoiceId() != null ? dto.getInvoiceId().trim() : null);

        nakladna.setVendorName(dto.getVendorName() != null ? dto.getVendorName().replace("\n", " ").trim() : null);
        nakladna.setVendorEdrpou(normalizeVendorCode(dto.getVendorEdrpou()));
        nakladna.setVendorIpn(normalizeVendorCode(dto.getVendorIpn()));

        // Нормалізація дати
        LocalDate parsedDate = normalizeDate(dto.getInvoiceDate());
        if (parsedDate != null) {
            nakladna.setInvoiceDate(parsedDate.toString()); // YYYY-MM-DD
        } else {
            nakladna.setInvoiceDate(dto.getInvoiceDate()); // Зберігаємо як є, якщо парсинг не вдався
        }

        nakladna.setTotalAmount(parseBigDecimal(dto.getTotalAmount()));
        nakladna.setFileName(dto.getFileName());
        nakladna.setUploadedTo1C(false);

        // Перевірка на дублікати перед обробкою товарів
        if (isDuplicate(nakladna)) {
            throw new DuplicateNakladnaException("Видаткова накладна з такими даними вже існує!");
        }

        // Обробка позицій
        if (dto.getProducts() != null) {
            dto.getProducts().forEach(productDto -> {
                NakladnaItemEntity item = new NakladnaItemEntity();
                item.setDescription(productDto.getDescription() != null ? productDto.getDescription().replace("\n", " ").trim() : null);

                // Quantity та Amount приходять вже як BigDecimal завдяки нашому DTO та Jackson
                item.setQuantity(productDto.getQuantity());
                item.setAmount(productDto.getAmount());

                item.setUnit(productDto.getUnit() != null ? productDto.getUnit().replace("|", "").trim() : null);
                item.setUnitPrice(parseBigDecimal(productDto.getUnitPrice()));

                nakladna.addItem(item);
            });
        }

        nakladnaRepository.save(nakladna);
    }

    @Transactional(readOnly = true)
    public boolean isDuplicate(NakladnaEntity nakladna) {
        Optional<NakladnaEntity> duplicate = Optional.empty();

        if (nakladna.getVendorEdrpou() != null && !nakladna.getVendorEdrpou().isBlank()) {
            duplicate = nakladnaRepository.findByInvoiceDateAndVendorEdrpouAndInvoiceId(
                    nakladna.getInvoiceDate(), nakladna.getVendorEdrpou(), nakladna.getInvoiceId());
        } else if (nakladna.getVendorIpn() != null && !nakladna.getVendorIpn().isBlank()) {
            duplicate = nakladnaRepository.findByInvoiceDateAndVendorIpnAndInvoiceId(
                    nakladna.getInvoiceDate(), nakladna.getVendorIpn(), nakladna.getInvoiceId());
        }

        return duplicate.isPresent();
    }

    private LocalDate normalizeDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        String cleanDate = rawDate.trim()
                .toLowerCase()
                .replaceAll("р\\.?\\s*$", "")
                .trim();

        if (cleanDate.matches("^\\d{1,2}\\.\\d{1,2}\\.\\d{4}$")) {
            try {
                return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern("d.M.yyyy"));
            } catch (DateTimeParseException e) {
                return null;
            }
        }

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

        String cleanAmount = rawAmount
                .replace(" ", "")
                .replace("\u00a0", "")
                .replace("'", "")
                .replace("\\", "")
                .replace("`", "")
                .replace(",", ".");

        try {
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeVendorCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return null;
        }
        String cleanCode = rawCode.replaceAll("\\D+", "");
        return cleanCode.isEmpty() ? null : cleanCode;
    }
}