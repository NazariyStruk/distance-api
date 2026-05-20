package com.fuchs.aktsParser;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acts")
@Data
public class AktEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numberDoc;
    private String dateDoc;
    private String typeDoc;
    private String nameSupplier;
    private String codeSupplier;
    private String ipnSupplier;
    private BigDecimal amountDoc;
    private BigDecimal taxDoc;
    private String includeTax;
    @Column(name = "file_name")
    private String fileName;

    @OneToMany(mappedBy = "act", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AktItemEntity> items = new ArrayList<>();

    public void addItem(AktItemEntity item) {
        items.add(item);
        item.setAct(this);
    }

}
