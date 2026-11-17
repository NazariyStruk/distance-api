package com.fuchs.invoicesParser;

import java.util.List;

/**
 * Відповідь /api/invoices/fill-missing — видно в історії запуску флоу.
 *
 * @param skipped    true, якщо документ не оброблявся (напр. не німецький постачальник)
 * @param foundInOcr скільки позицій з ціною знайдено в OCR
 * @param alreadySaved скільки з них уже було в БД (збережені основним For each)
 * @param added      номери рядків, які були пропущені AI Builder і тепер дозбережені
 * @param failed     номери рядків, які не вдалося зберегти (деталі в логах)
 */
public record FillMissingResponseDto(
        boolean skipped,
        int foundInOcr,
        int alreadySaved,
        List<Integer> added,
        List<Integer> failed
) {
    public static FillMissingResponseDto skippedResponse() {
        return new FillMissingResponseDto(true, 0, 0, List.of(), List.of());
    }
}
