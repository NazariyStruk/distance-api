package com.fuchs.nakladniParser;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class NakladnaItemDto {
    @JsonProperty("description")
    private String description;

    @JsonProperty("quantity")
    private BigDecimal quantity; // Приймає і цілі числа (36), і дробові через крапку (0.5)

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("unitPrice")
    private String unitPrice; // Залишаємо String для парсингу ком ("370,80")

    @JsonProperty("amount")
    private BigDecimal amount; // Чисте число з JSON (13348.8 або 6160)
}
