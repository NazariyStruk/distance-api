package com.fuchs.aktsParser;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "act_items")
@Data
public class AktItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "act_id")
    private AktEntity act;

    private String articul;
    @Column(columnDefinition = "TEXT")
    private String description;
    private Integer quantity;
    private String units;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal tax;
}
