package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryCompletionRepository
    extends JpaRepository<DeliveryCompletion, Long> {

    Optional<DeliveryCompletion> findByDeliveryId(
        Long deliveryId
    );
}