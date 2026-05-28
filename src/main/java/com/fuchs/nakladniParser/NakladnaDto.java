package com.fuchs.nakladniParser;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class NakladnaDto {
    @JsonProperty("DocumentType")
    private String documentType;

    @JsonProperty("InvoiceId")
    private String invoiceId;

    @JsonProperty("InvoiceDate")
    private String invoiceDate;

    @JsonProperty("VendorName")
    private String vendorName;

    @JsonProperty("VendorEDRPOU")
    private String vendorEdrpou;

    @JsonProperty("VendorIPN")
    private String vendorIpn;

    @JsonProperty("TotalAmount")
    private String totalAmount;

    private String fileName;

    @JsonProperty("Products")
    private List<NakladnaItemDto> products;
}
