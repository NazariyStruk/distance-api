package com.fuchs.aktsParser;

import lombok.Data;

@Data
public class AktItemDto {
    private String articul;
    private String description;
    private String quantity;
    private String units;
    private String price;
    private String amount;
    private String tax;
}
