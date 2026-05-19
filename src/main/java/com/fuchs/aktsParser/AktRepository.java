package com.fuchs.aktsParser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AktRepository extends JpaRepository<AktEntity, Long> {

    // 1. Пошук, якщо є код постачальника (ЄДРПОУ)
    Optional<AktEntity> findByDateDocAndCodeSupplierAndNumberDoc(String dateDoc, String codeSupplier, String numberDoc);

    // 2. Пошук, якщо коду немає, а є ІПН
    Optional<AktEntity> findByDateDocAndIpnSupplierAndNumberDoc(String dateDoc, String ipnSupplier, String numberDoc);
}
