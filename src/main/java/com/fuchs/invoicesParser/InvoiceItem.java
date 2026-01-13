package com.fuchs.invoicesParser;

import jakarta.persistence.GeneratedValue;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Data
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "line_number")
    private Integer lineNumber; // Наприклад: 10, 250

    @Column(name = "price")
    private BigDecimal price;   // Наприклад: 232.42

    @Column(name = "invoice_number")
    private String number;      // Приходить з power automate

    @Column(name = "vendor_tax_id")
    private String vendorTaxId;
    //ToDO:  артикул(перших 10 цифр у дескр), назва опшинал, уточнити в Юри чи нам треба

    @Column(name = "raw_descr", length = 1000)
    private String rawDescr;    // Зберігаємо оригінал на всяк випадок
}
