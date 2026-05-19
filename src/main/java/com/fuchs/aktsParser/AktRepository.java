package com.fuchs.aktsParser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AktRepository extends JpaRepository<AktEntity, Long> {
}
