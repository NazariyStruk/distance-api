package com.fuchs.invoicesParser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Запит для /api/invoices/fill-missing.
 * number / vendorTaxId / date — ті самі значення, що флоу шле в /cleanup і /reset-status.
 * readResults — сирий OCR з AI Builder (body/responsev2/predictionOutput/readResults).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FillMissingRequestDto {
    private String number;
    private String vendorTaxId;
    private LocalDate date;
    private List<OcrLineDto> readResults;
}
