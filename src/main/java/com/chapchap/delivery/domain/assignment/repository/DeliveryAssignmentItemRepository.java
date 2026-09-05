package com.chapchap.delivery.domain.assignment.repository;

import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryAssignmentItemRepository
    extends JpaRepository<DeliveryAssignmentItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT dai
        FROM DeliveryAssignmentItem dai
        JOIN dai.assignment da
        WHERE da.deliveryGroup.id = :deliveryGroupId
            AND dai.deletedAt IS NULL
        ORDER BY dai.id ASC
    """)
    List<DeliveryAssignmentItem> findAllByDeliveryGroupIdForUpdate(
        @Param("deliveryGroupId") Long deliveryGroupId
    );

    @Query("""
        SELECT dai
        FROM DeliveryAssignmentItem dai
        JOIN FETCH dai.delivery d
        WHERE dai.assignment.id = :assignmentId
            AND dai.deletedAt IS NULL
        ORDER BY dai.id ASC
    """)
    List<DeliveryAssignmentItem> findAllByAssignmentIdWithDelivery(
        @Param("assignmentId") Long assignmentId
    );
}