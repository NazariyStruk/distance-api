package com.fuchs.invoicesParser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    void deleteByNumberAndVendorTaxIdAndDate(String number, String vendorTaxId, LocalDate date);

    List<InvoiceItem> findAllByNumberAndVendorTaxIdAndDate(String number, String vendorTaxId, LocalDate date);
}
