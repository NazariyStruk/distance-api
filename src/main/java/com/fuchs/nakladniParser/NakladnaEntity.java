package com.fuchs.nakladniParser;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nakladna")
@Getter
@Setter
public class NakladnaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "invoice_id", nullable = false)
    private String invoiceId;

    @Column(name = "invoice_date")
    private String invoiceDate;

    @Column(name = "vendor_name", length = 255)
    private String vendorName;

    @Column(name = "vendor_edrpou")
    private String vendorEdrpou;

    @Column(name = "vendor_ipn")
    private String vendorIpn;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "uploaded_to_1c")
    private boolean uploadedTo1C;

    @OneToMany(mappedBy = "nakladna", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NakladnaItemEntity> items = new ArrayList<>();

    public void addItem(NakladnaItemEntity item) {
        items.add(item);
        item.setNakladna(this);
    }
}
