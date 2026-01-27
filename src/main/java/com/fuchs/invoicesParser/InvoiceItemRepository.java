package com.fuchs.invoicesParser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    @Modifying
    @Query("DELETE FROM InvoiceItem i WHERE i.number = :number AND i.vendorTaxId = :vendorTaxId AND i.date = :date")
    void deleteCustom(@Param("number") String number,
                      @Param("vendorTaxId") String vendorTaxId,
                      @Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE InvoiceItem i SET i.updatedBy1c = false WHERE i.number = :number AND i.vendorTaxId = :vendorTaxId AND i.date = :date")
    void resetStatusCustom(@Param("number") String number,
                           @Param("vendorTaxId") String vendorTaxId,
                           @Param("date") LocalDate date);
}
