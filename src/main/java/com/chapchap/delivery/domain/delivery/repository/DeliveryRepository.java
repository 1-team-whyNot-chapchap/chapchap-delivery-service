package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository
    extends JpaRepository<Delivery, Long> {

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
}