package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.DeliveryRecipientSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DeliveryRecipientSnapshotRepository
    extends JpaRepository<DeliveryRecipientSnapshot, Long> {

    @Query("""
        SELECT drs
        FROM DeliveryRecipientSnapshot drs
        WHERE drs.deliveryId IN :deliveryIds
    """)
    List<DeliveryRecipientSnapshot> findAllByDeliveryIdIn(
        @Param("deliveryIds") Collection<Long> deliveryIds
    );
}