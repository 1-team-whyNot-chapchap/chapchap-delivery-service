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
            AND dai.assignment.deliveryGroup.deletedAt IS NULL
            AND d.deliveryGroup.deletedAt IS NULL
        ORDER BY dai.id ASC
    """)
    List<DeliveryAssignmentItem> findAllByAssignmentIdWithDelivery(
        @Param("assignmentId") Long assignmentId
    );

    @Query("""
        SELECT dai
        FROM DeliveryAssignmentItem dai
        JOIN FETCH dai.assignment da
        JOIN FETCH da.rider
        JOIN FETCH dai.delivery d
        WHERE da.deliveryGroup.id IN :deliveryGroupIds
          AND dai.deletedAt IS NULL
          AND da.deletedAt IS NULL
        ORDER BY dai.id ASC
    """)
    List<DeliveryAssignmentItem> findAllByDeliveryGroupIdIn(
        @Param("deliveryGroupIds") List<Long> deliveryGroupIds
    );
}
