package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DeliveryGroupRepository extends JpaRepository<DeliveryGroup, Long> {
    Optional<DeliveryGroup> findByDeliveryDateAndSlot(
        LocalDate deliveryDate
        , DeliverySlot slot
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT dg
        FROM DeliveryGroup dg
        WHERE dg.id = :deliveryGroupId
            AND dg.deletedAt IS NULL
    """)
    Optional<DeliveryGroup> findByIdForUpdate(
        @Param("deliveryGroupId") Long deliveryGroupId
    );

    @Query("""
        SELECT dg.id
        FROM DeliveryGroup dg
        WHERE dg.deliveryDate = :deliveryDate
            AND dg.status = :status
            AND dg.autoAssignmentCompletedAt IS NULL
            AND dg.deletedAt IS NULL
        ORDER BY dg.id ASC
    """)
    List<Long> findAutoAssignmentTargetIds(
        @Param("deliveryDate") LocalDate deliveryDate
        , @Param("status") DeliveryGroupStatus status
    );
}