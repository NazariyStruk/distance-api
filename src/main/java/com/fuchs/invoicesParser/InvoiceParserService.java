package com.fuchs.invoicesParser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InvoiceParserService {

    private final InvoiceItemRepository repository;

    public InvoiceParserService(InvoiceItemRepository repository) {
        this.repository = repository;
    }

    @Transactional // Обов'язково для операцій delete/update
    public void deleteAllInvoicesRelatedEntries(InvoiceRequestDto dto) {
        if (dto.getNumber() != null && dto.getVendorTaxId() != null && dto.getDate() != null) {
            repository.deleteCustom(
                    dto.getNumber(),
                    dto.getVendorTaxId(),
                    dto.getDate()
            );
        }
    }

    @Transactional
    public void resetSyncStatus(InvoiceRequestDto dto) {
        // Перевіряємо, чи прийшли ключові поля
        if (dto.getNumber() != null && dto.getVendorTaxId() != null && dto.getDate() != null) {
            repository.resetStatusCustom(
                    dto.getNumber(),
                    dto.getVendorTaxId(),
                    dto.getDate()
            );
        }
    }

    @Transactional
    public void parseAndSave(InvoiceRequestDto dto) {
        String descr = dto.getDescr();

        // 1. Перевірка на валідність
        if (descr == null || !descr.contains("/")) {
            throw new IllegalArgumentException("Invalid format: '/' not found in description");
        }

        // 2. Витягуємо частину рядка ДО символу "/"
        // Приклад: "10 Material: ... 169.61 "
        String partBeforeSlash = descr.substring(0, descr.indexOf("/")).trim();

        // 3. Розбиваємо по пробілах, щоб дістати перше та останнє значення
        String[] tokens = partBeforeSlash.split("\\s+");

        if (tokens.length < 2) {
            // Логіка обробки, якщо рядок занадто короткий (хоча за умовою ціна і номер є завжди)
            throw new IllegalArgumentException("Cannot extract line number and price");
        }

        // 4. Парсимо Line Number (перший токен)
        Integer lineNumber = Integer.parseInt(tokens[0]);

        // 5. Парсимо Price (останній токен перед слешем)
        String priceStr = tokens[tokens.length - 1];
        BigDecimal price = normalizePrice(priceStr);
        String extractedArticul = extractArticul(descr);
        BigDecimal amount = null;
        String units = null;

        if (extractedArticul != null) {
            // Шукаємо позицію артикулу в масиві токенів
            for (int i = 0; i < tokens.length; i++) {
                // Якщо знайшли токен, який дорівнює нашому артикулу
                if (tokens[i].equals(extractedArticul)) {
                    // Перевіряємо, чи є наступні елементи (Amount та Units)
                    if (i + 2 < tokens.length) {
                        String rawAmount = tokens[i + 1]; // Наступний після артикулу (напр. "60,00" або "29")
                        units = tokens[i + 2];            // Через один після артикулу (напр. "K03" або "EA")

                        amount = parseAmount(rawAmount);
                    }
                    break; // Знайшли і виходимо з циклу
                }
            }
        }

        // 6. Створюємо та зберігаємо об'єкт
        InvoiceItem item = new InvoiceItem();
        item.setLineNumber(lineNumber);
        item.setPrice(price);
        item.setNumber(dto.getNumber());
        item.setRawDescr(descr);
        item.setVendorTaxId(dto.getVendorTaxId());
        item.setDate(dto.getDate());
        item.setArticul(extractedArticul);
        item.setAmount(amount);
        item.setUnits(units);
        item.setUpdatedBy1c(true);
        repository.save(item);
    }

    private BigDecimal parseAmount(String rawAmount) {
        try {
            // Замінюємо кому на крапку: "60,00" -> "60.00"
            String cleanAmount = rawAmount.replace(",", ".");
            // Парсимо як Double
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse amount: " + rawAmount);
            return null;
        }
    }


    // Метод для пошуку артикулу (9 цифр підряд)
    private String extractArticul(String text) {
        if (text == null) return null;

        // Регулярний вираз: \b означає межу слова, \d{9} означає рівно 9 цифр
        // Це знайде "602003027", але пропустить ціну "169.61" або малі числа "250"
        Pattern pattern = Pattern.compile("\\b\\d{9}\\b");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(); // Повертає перше знайдене співпадіння
        }
        return null;
    }

    // Метод для перетворення "232,42", "169.61", "1,772.77" у BigDecimal
    private BigDecimal normalizePrice(String rawPrice) {
        // Якщо є і кома, і крапка (напр. 1,772.77) -> прибираємо коми (тисячні)
        if (rawPrice.contains(",") && rawPrice.contains(".")) {
            rawPrice = rawPrice.replace(",", "");
        }
        // Якщо тільки кома (напр. 232,42) -> замінюємо на крапку
        else if (rawPrice.contains(",")) {
            rawPrice = rawPrice.replace(",", ".");
        }

        return new BigDecimal(rawPrice);
    }
}
