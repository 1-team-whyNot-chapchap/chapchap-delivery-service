package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    @Query(
        value = """
            SELECT COUNT(*)
            FROM deliveries
            WHERE source_order_id = :sourceOrderId
            """
        , nativeQuery = true
    )
    long countBySourceOrderIdIncludingDeleted(
        @Param("sourceOrderId") String sourceOrderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d
        FROM Delivery d
        WHERE d.deliveryGroup.id = :deliveryGroupId
        ORDER BY d.id ASC
    """)
    List<Delivery> findAllByDeliveryGroupIdForUpdate(
        @Param("deliveryGroupId") Long deliveryGroupId
    );
}