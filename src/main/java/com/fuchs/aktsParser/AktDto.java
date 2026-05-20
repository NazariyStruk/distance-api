package com.fuchs.aktsParser;

import lombok.Data;

import java.util.List;

@Data
public class AktDto {
    private String numberDoc;
    private String dateDoc;
    private String typeDoc;
    private String nameSupplier;
    private String codeSupplier;
    private String ipnSupplier;
    private String amountDoc;
    private String taxDoc;
    private String includeTax;
    private String fileName;
    private List<AktItemDto> items;

}
