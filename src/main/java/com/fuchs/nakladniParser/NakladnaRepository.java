package com.fuchs.nakladniParser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NakladnaRepository extends JpaRepository<NakladnaEntity, Long> {

    // Пошук за датою, ЄДРПОУ та номером
    Optional<NakladnaEntity> findByInvoiceDateAndVendorEdrpouAndInvoiceId(String invoiceDate, String vendorEdrpou, String invoiceId);

    // Пошук за датою, ІПН та номером
    Optional<NakladnaEntity> findByInvoiceDateAndVendorIpnAndInvoiceId(String invoiceDate, String vendorIpn, String invoiceId);

    // НОВИЙ МЕТОД: Фолбек-пошук за назвою, якщо коди відсутні
    Optional<NakladnaEntity> findByInvoiceDateAndInvoiceIdAndVendorName(String invoiceDate, String invoiceId, String vendorName);
}