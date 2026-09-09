package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryDelay;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryDelayRepository
    extends JpaRepository<DeliveryDelay, Long> {

    Optional<DeliveryDelay> findByDeliveryId(
        Long deliveryId
    );

    List<DeliveryDelay> findAllByDeliveryIdIn(
        Collection<Long> deliveryIds
    );

    @Modifying
    @Query(value = """
        INSERT INTO delivery_delays (delivery_id, detected_at, created_at, updated_at)
        VALUES (:deliveryId, :detectedAt, :detectedAt, :detectedAt)
        ON DUPLICATE KEY UPDATE id = id
    """, nativeQuery = true)
    int insertIfAbsent(
        @Param("deliveryId") Long deliveryId
        , @Param("detectedAt") LocalDateTime detectedAt
    );
}
