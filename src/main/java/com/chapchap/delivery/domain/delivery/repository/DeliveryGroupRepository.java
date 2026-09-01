package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DeliveryGroupRepository
    extends JpaRepository<DeliveryGroup, Long> {

    Optional<DeliveryGroup> findByDeliveryDateAndSlot(
        LocalDate deliveryDate
        , DeliverySlot slot
    );
}