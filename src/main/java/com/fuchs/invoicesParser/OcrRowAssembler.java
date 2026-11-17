package com.fuchs.invoicesParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Збирає рядки позицій з OCR-тексту AI Builder (readResults) для order confirmation FUCHS Germany.
 *
 * OCR повертає кожну колонку таблиці окремим рядком з координатами, напр. для позиції 460:
 *   "460" (left 0.051, top 0.7519), "602183736" (0.099, 0.7530), "36 K12" (0.522, 0.7517),
 *   "375.84 KG" (0.643), "434.85 / 100 KG" (0.751), "1,634.34" (0.919)
 *
 * Позиція = номер рядка в лівій колонці + 9-значний артикул поруч на тій самій висоті.
 * Усі OCR-рядки цієї сторінки на тій самій висоті, відсортовані зліва направо і склеєні пробілом,
 * дають той самий текст, що AI Builder повертає в items[].rowText:
 *   "460 602183736 36 K12 375.84 KG 434.85 / 100 KG 1,634.34"
 *
 * Клас без Spring-залежностей — щоб легко тестувати.
 */
public final class OcrRowAssembler {

    /** Допуск по вертикалі (частка висоти сторінки), щоб вважати OCR-рядки одним рядком таблиці. */
    static final double ROW_TOLERANCE = 0.006;
    /** Номер позиції стоїть у самій лівій колонці "Item". */
    static final double ITEM_NUMBER_MAX_LEFT = 0.075;
    /** Артикул стоїть у колонці "Material". */
    static final double ARTICUL_MIN_LEFT = 0.08;
    static final double ARTICUL_MAX_LEFT = 0.20;

    private static final Pattern ITEM_NUMBER = Pattern.compile("\\d{2,4}");
    private static final Pattern ARTICUL = Pattern.compile("\\d{9}");
    private static final Pattern AMOUNT = Pattern.compile("[\\d.,]+");

    private OcrRowAssembler() {
    }

    /**
     * @param lineNumber номер позиції (10, 20, ... 460)
     * @param articul    9-значний артикул
     * @param rowText    рядок у форматі items[].rowText
     * @param textPrice  частина з ціною, напр. "434.85 / 100 KG" (null, якщо ціни немає)
     * @param amount     сума позиції, напр. "1,634.34" (null, якщо ціни немає)
     */
    public record AssembledRow(int lineNumber, String articul, String rowText, String textPrice, String amount) {
        /** Позиції без ціни ("limited quantities ...") основний флоу теж не зберігає. */
        public boolean isPriced() {
            return textPrice != null && amount != null;
        }
    }

    public static List<AssembledRow> assemble(List<OcrLineDto> readResults) {
        List<AssembledRow> result = new ArrayList<>();
        if (readResults == null || readResults.isEmpty()) {
            return result;
        }

        Map<Integer, List<OcrLineDto>> byPage = readResults.stream()
                .filter(OcrRowAssembler::hasCoordinates)
                .collect(Collectors.groupingBy(l -> l.getLocation().getPage(), LinkedHashMap::new, Collectors.toList()));

        byPage.keySet().stream().sorted().forEach(page -> {
            List<OcrLineDto> lines = byPage.get(page);
            lines.stream()
                    .filter(OcrRowAssembler::isItemNumberCell)
                    .sorted(Comparator.comparingDouble(OcrRowAssembler::top))
                    .forEach(itemCell -> buildRow(itemCell, lines).ifPresent(result::add));
        });

        return result;
    }

    private static Optional<AssembledRow> buildRow(OcrLineDto itemCell, List<OcrLineDto> pageLines) {
        double rowTop = top(itemCell);

        List<OcrLineDto> sameRow = pageLines.stream()
                .filter(l -> Math.abs(top(l) - rowTop) <= ROW_TOLERANCE)
                .sorted(Comparator.comparingDouble(OcrRowAssembler::left))
                .toList();

        OcrLineDto articulCell = sameRow.stream()
                .filter(OcrRowAssembler::isArticulCell)
                .findFirst()
                .orElse(null);
        if (articulCell == null) {
            // Номер без артикулу поруч — це не позиція таблиці (напр. випадкове число в тексті)
            return Optional.empty();
        }

        List<String> cells = sameRow.stream()
                .map(l -> l.getText().trim())
                .filter(t -> !t.isEmpty())
                .toList();

        String rowText = String.join(" ", cells);

        String textPrice = cells.stream().filter(c -> c.contains("/")).findFirst().orElse(null);
        String lastCell = cells.get(cells.size() - 1);
        String amount = (textPrice != null && !lastCell.equals(textPrice) && AMOUNT.matcher(lastCell).matches())
                ? lastCell
                : null;

        return Optional.of(new AssembledRow(
                Integer.parseInt(itemCell.getText().trim()),
                articulCell.getText().trim(),
                rowText,
                textPrice,
                amount
        ));
    }

    private static boolean isItemNumberCell(OcrLineDto l) {
        return l.getText() != null
                && ITEM_NUMBER.matcher(l.getText().trim()).matches()
                && left(l) <= ITEM_NUMBER_MAX_LEFT;
    }

    private static boolean isArticulCell(OcrLineDto l) {
        return l.getText() != null
                && ARTICUL.matcher(l.getText().trim()).matches()
                && left(l) >= ARTICUL_MIN_LEFT
                && left(l) <= ARTICUL_MAX_LEFT;
    }

    private static boolean hasCoordinates(OcrLineDto l) {
        return l != null
                && l.getText() != null
                && l.getLocation() != null
                && l.getLocation().getPage() != null
                && l.getLocation().getBoundingBox() != null
                && Objects.nonNull(l.getLocation().getBoundingBox().getLeft())
                && Objects.nonNull(l.getLocation().getBoundingBox().getTop());
    }

    private static double top(OcrLineDto l) {
        return l.getLocation().getBoundingBox().getTop();
    }

    private static double left(OcrLineDto l) {
        return l.getLocation().getBoundingBox().getLeft();
    }
}
