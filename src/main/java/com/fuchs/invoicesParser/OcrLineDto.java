package com.fuchs.invoicesParser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Один OCR-рядок з AI Builder:
 * outputs('Process_invoices')?['body/responsev2/predictionOutput/readResults'][*]
 *
 * Приклад (службові "@odata.type" поля ігноруються):
 * { "text": "602183736",
 *   "location": { "page": 13, "boundingBox": { "left": 0.0991, "top": 0.753, "width": ..., "height": 0.0097 } } }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrLineDto {
    private String text;
    private Location location;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private Integer page;
        private BoundingBox boundingBox;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoundingBox {
        private Double left;
        private Double top;
        private Double width;
        private Double height;
    }
}
