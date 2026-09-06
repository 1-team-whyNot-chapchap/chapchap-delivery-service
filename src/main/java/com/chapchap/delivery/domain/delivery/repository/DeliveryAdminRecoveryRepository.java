package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryAdminRecovery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryAdminRecoveryRepository
    extends JpaRepository<DeliveryAdminRecovery, Long> {

    Optional<DeliveryAdminRecovery> findByDeliveryId(Long deliveryId);
}
