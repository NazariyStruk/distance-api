package com.fuchs.nakladniParser;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "nakladna_items")
@Getter
@Setter
public class NakladnaItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nakladna_id", nullable = false)
    private NakladnaEntity nakladna;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    private BigDecimal quantity;
    private String unit;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    private BigDecimal amount;
}
