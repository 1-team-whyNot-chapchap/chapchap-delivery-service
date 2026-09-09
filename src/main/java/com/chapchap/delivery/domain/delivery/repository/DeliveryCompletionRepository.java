package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryCompletionRepository
    extends JpaRepository<DeliveryCompletion, Long> {

    Optional<DeliveryCompletion> findByDeliveryId(
        Long deliveryId
    );

    List<DeliveryCompletion> findAllByDeliveryIdIn(
        Collection<Long> deliveryIds
    );
}
