package com.fuchs.invoicesParser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
            return;
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

        String priceStr = dto.getTextPrice();

        if (priceStr == null) {
            // Шукаємо в токенах (логіка з попередніх кроків)
            for (int i = tokens.length - 1; i >= 0; i--) {
                BigDecimal possible = normalizePrice(tokens[i]);
                if (possible != null) {
                    priceStr = tokens[i];
                    break;
                }
            }
        }

        if (priceStr != null) {
            // 1. Відрізаємо все після слеша ("197,11 EUR/100 KG" -> "197,11 EUR")
            if (priceStr.contains("/")) {
                priceStr = priceStr.substring(0, priceStr.indexOf("/")).trim();
            }

            // 2. Відрізаємо валюту (беремо перше слово до пробілу)
            // "197,11 EUR" -> "197,11"
            String[] priceTokens = priceStr.split("\\s+");
            if (priceTokens.length > 0) {
                // Перевіряємо, чи перший токен схожий на число (містить цифри)
                if (priceTokens[0].matches(".*\\d.*")) {
                    priceStr = priceTokens[0];
                }
            }
        }

        BigDecimal price = normalizePrice(priceStr);
        if (price == null) {
            price = extractPriceFromDescriptionFallback(descr);
        }
        String extractedArticul = extractArticul(descr);
        BigDecimal quantity = null;
        String units = null;
        String unitPriceValue = null;
        BigDecimal weight = null; // Нове поле

        boolean isGerman = dto.getVendorTaxId() != null && dto.getVendorTaxId().startsWith("DE");

        if (extractedArticul != null) {
            String[] allTokens = descr.split("\\s+");
            // Шукаємо позицію артикулу в масиві токенів
            for (int i = 0; i < allTokens.length; i++) {
                // Якщо знайшли токен, який дорівнює нашому артикулу
                if (allTokens[i].equals(extractedArticul)) {
                    // Перевіряємо, чи є наступні елементи (Amount та Units)
                    if (i + 2 < tokens.length) {
                        String rawQuantity = tokens[i + 1]; // Наступний після артикулу (напр. "60,00" або "29")
                        units = allTokens[i + 2];            // Через один після артикулу (напр. "K03" або "EA")

                        if (units.contains("/")) {
                            units = units.substring(0, units.indexOf("/"));
                        }
                        quantity = parseQuantity(rawQuantity);
                        if (isGerman && i + 3 < allTokens.length) {
                            String rawWeight = allTokens[i + 3];

                            if (rawWeight != null && rawWeight.matches("\\d{1,3},\\d{3}")) {
                                if (descr.contains(".")) {
                                    // Якщо контекст рядка англійський (є крапки), то кома в "1,688" — це тисячі.
                                    // Просто видаляємо її.
                                    rawWeight = rawWeight.replace(",", ""); // Стає "1688"
                                }
                            }
                            // Перевіряємо, чи це число, бо іноді може бути текст
                            weight = normalizePrice(rawWeight);
                        }
                    }
                    break; // Знайшли і виходимо з циклу
                }
            }
        }

        if (!isGerman) {
            weight = extractPolishWeight(descr);
            if (weight == null) {
                weight = processWeight(dto.getWeightPL());
            }
        }

        BigDecimal totalAmount = normalizePrice(dto.getAmount());


        if (descr.contains("/ 100 KG") || descr.contains("/100 KG")) {
            unitPriceValue = "100 KG";
        } else if (descr.contains("/100 L") || descr.contains("/ 100 L")) {
            unitPriceValue = "100 L";
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
        item.setQuantity(quantity);
        item.setAmount(totalAmount);
        item.setUnits(units);
        item.setUnitPrice(unitPriceValue);
        item.setUpdatedBy1c(true);
        item.setWeight(weight);
        repository.save(item);
    }

    private BigDecimal extractPriceFromDescriptionFallback(String descr) {
        int index = -1;

        // Шукаємо якорі в порядку пріоритету
        if (descr.contains("EUR/100")) {
            index = descr.indexOf("EUR/100"); // Для польських "... 179,49 EUR/100 KG"
        } else if (descr.contains("/ 100")) {
            index = descr.indexOf("/ 100");  // Для німецьких "... 507.92 / 100 KG"
        } else if (descr.contains("/100")) {
            index = descr.indexOf("/100");    // Загальний фолбек
        }

        if (index != -1) {
            // Беремо текст ДО якоря: "... 360,00 L 179,49 "
            String textBeforeAnchor = descr.substring(0, index).trim();
            // Розбиваємо по пробілах і беремо ОСТАННЄ слово
            String[] tokens = textBeforeAnchor.split("\\s+");
            if (tokens.length > 0) {
                String potentialPrice = tokens[tokens.length - 1];
                return normalizePrice(potentialPrice);
            }
        }
        return null;
    }

    // Спеціальний метод для пошуку ваги в польських інвойсах
    private BigDecimal extractPolishWeight(String descr) {
        try {
            String lowerDescr = descr.toLowerCase();
            // Враховуємо опечатку "weigth" і правильне "weight"
            int index = lowerDescr.indexOf("net weigth:");
            if (index == -1) {
                index = lowerDescr.indexOf("net weight:");
            }

            if (index != -1) {
                // Відрізаємо все до двокрапки включно
                String textAfterLabel = descr.substring(index).split(":")[1].trim();
                // Беремо перший токен (це має бути число)
                String rawWeight = textAfterLabel.split("\\s+")[0];
                return normalizePrice(rawWeight);
            }
        } catch (Exception e) {
            // Якщо щось пішло не так, повертаємо null
        }
        return null;
    }

    private BigDecimal processWeight(String rawWeight) {
        if (rawWeight == null) return null;
        try {
            // Крок 1: Видаляємо все, крім цифр, крапок та ком
            // "2.843,840 KG" -> "2.843,840"
            // "17,960 KG" -> "17,960"
            String cleaned = rawWeight.replaceAll("[^0-9,.]", "");

            // Крок 2: Нормалізація роздільників (Європа -> Java)
            if (cleaned.contains(".") && cleaned.contains(",")) {
                // Випадок "2.843,840": видаляємо крапку (тисячі), міняємо кому на крапку
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else if (cleaned.contains(",")) {
                // Випадок "519,000": міняємо кому на крапку
                cleaned = cleaned.replace(",", ".");
            }

            // Крок 3: Створення BigDecimal
            BigDecimal bd = new BigDecimal(cleaned);

            // Крок 4: Встановлюємо 2 знаки після коми (округлення HALF_UP)
            return bd.setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            System.err.println("Failed to parse weight: " + rawWeight);
            return null;
        }
    }

    private BigDecimal parseQuantity(String rawQuantity) {
        if (rawQuantity == null) return null;
        try {
            // 1. Чистимо рядок: "60,00" -> "60.00"
            String cleanAmount = rawQuantity.replace(",", ".");

            // 2. Створюємо BigDecimal
            BigDecimal bd = new BigDecimal(cleanAmount);

            // 3. Встановлюємо 2 знаки після коми.
            // HALF_UP - це стандартне "шкільне" округлення (1.555 -> 1.56, 1.554 -> 1.55)
            return bd.setScale(2, RoundingMode.HALF_UP);

        } catch (NumberFormatException e) {
            System.err.println("Failed to parse quantity: " + rawQuantity);
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
    private BigDecimal normalizePrice(String rawValue) {
        if (rawValue == null) return null;
        try {
            rawValue = rawValue.replace(" ", "").replace("\u00A0", "");
            // "1,580.60" -> "1580.60"
            if (rawValue.contains(",") && rawValue.contains(".")) {

                if (rawValue.indexOf(".") < rawValue.indexOf(",")) {
                    rawValue = rawValue.replace(".", "").replace(",", ".");
                } else {
                    rawValue = rawValue.replace(",", "");
                }
            } else if (rawValue.contains(",")) {
                rawValue = rawValue.replace(",", ".");
            }
            BigDecimal bd = new BigDecimal(rawValue);

            // ДОДАНО: Примусово встановлюємо 2 знаки після коми
            return bd.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }
}
