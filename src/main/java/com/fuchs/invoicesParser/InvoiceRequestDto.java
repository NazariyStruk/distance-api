package com.fuchs.invoicesParser;

import lombok.Data;

@Data
public class InvoiceRequestDto {
    private String descr;
    private String number;
    private String vendorTaxId;
}
