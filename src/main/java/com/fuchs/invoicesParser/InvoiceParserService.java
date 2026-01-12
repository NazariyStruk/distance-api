//package com.fuchs.invoicesParser;
//
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//
//@Service
//public class InvoiceParserService {
//
//    private final InvoiceItemRepository repository;
//
//    public InvoiceParserService(InvoiceItemRepository repository) {
//        this.repository = repository;
//    }
//
//    public void parseAndSave(InvoiceRequestDto dto) {
//        String descr = dto.getDescr();
//
//        // 1. Перевірка на валідність
//        if (descr == null || !descr.contains("/")) {
//            throw new IllegalArgumentException("Invalid format: '/' not found in description");
//        }
//
//        // 2. Витягуємо частину рядка ДО символу "/"
//        // Приклад: "10 Material: ... 169.61 "
//        String partBeforeSlash = descr.substring(0, descr.indexOf("/")).trim();
//
//        // 3. Розбиваємо по пробілах, щоб дістати перше та останнє значення
//        String[] tokens = partBeforeSlash.split("\\s+");
//
//        if (tokens.length < 2) {
//            // Логіка обробки, якщо рядок занадто короткий (хоча за умовою ціна і номер є завжди)
//            throw new IllegalArgumentException("Cannot extract line number and price");
//        }
//
//        // 4. Парсимо Line Number (перший токен)
//        Integer lineNumber = Integer.parseInt(tokens[0]);
//
//        // 5. Парсимо Price (останній токен перед слешем)
//        String priceStr = tokens[tokens.length - 1];
//        BigDecimal price = normalizePrice(priceStr);
//
//        // 6. Створюємо та зберігаємо об'єкт
//        InvoiceItem item = new InvoiceItem();
//        item.setLineNumber(lineNumber);
//        item.setPrice(price);
//        item.setNumber(dto.getNumber());
//        item.setRawDescr(descr);
//        item.setVendorTaxId(dto.getVendorTaxId());
////TODO перед сейвом перевіряти по invoiceNum and VendorTAxNum. Якщор є ,скіп (та сповіщення badRequest) .Якщо нема - сейв
//        repository.save(item);
//    }
//
//    // Метод для перетворення "232,42", "169.61", "1,772.77" у BigDecimal
//    private BigDecimal normalizePrice(String rawPrice) {
//        // Якщо є і кома, і крапка (напр. 1,772.77) -> прибираємо коми (тисячні)
//        if (rawPrice.contains(",") && rawPrice.contains(".")) {
//            rawPrice = rawPrice.replace(",", "");
//        }
//        // Якщо тільки кома (напр. 232,42) -> замінюємо на крапку
//        else if (rawPrice.contains(",")) {
//            rawPrice = rawPrice.replace(",", ".");
//        }
//
//        return new BigDecimal(rawPrice);
//    }
//}
