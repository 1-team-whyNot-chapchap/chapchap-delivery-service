package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.type.DeliverySlotCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliverySlotRepository extends JpaRepository<DeliverySlot, Long> {
    Optional<DeliverySlot> findByCodeAndDeletedAtIsNull(
        DeliverySlotCode code
    );
}
