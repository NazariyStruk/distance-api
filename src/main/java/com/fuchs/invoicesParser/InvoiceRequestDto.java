package com.fuchs.invoicesParser;

import lombok.Data;
import java.time.LocalDate;

@Data
public class InvoiceRequestDto {
    private String descr;
    private String number;
    private String vendorTaxId;
    private LocalDate date;
    private String amount;
    private String textPrice;
}
