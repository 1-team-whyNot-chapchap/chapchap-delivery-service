package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryFailureRepository
    extends JpaRepository<DeliveryFailure, Long> {

    Optional<DeliveryFailure> findByDeliveryId(
        Long deliveryId
    );

    List<DeliveryFailure> findAllByDeliveryIdIn(
        Collection<Long> deliveryIds
    );
}
