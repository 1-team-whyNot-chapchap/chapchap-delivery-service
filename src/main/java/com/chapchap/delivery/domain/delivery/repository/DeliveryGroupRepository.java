package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DeliveryGroupRepository extends JpaRepository<DeliveryGroup, Long> {
    Optional<DeliveryGroup> findByDeliveryDateAndSlot(
        LocalDate deliveryDate
        , DeliverySlot slot
    );

    @Query(
        value = """
            SELECT dg
            FROM DeliveryGroup dg
            JOIN FETCH dg.slot s
            WHERE dg.deletedAt IS NULL
              AND (:deliveryDate IS NULL OR dg.deliveryDate = :deliveryDate)
              AND (:slotCode IS NULL OR s.code = :slotCode)
              AND (:status IS NULL OR dg.status = :status)
        """
        , countQuery = """
            SELECT COUNT(dg)
            FROM DeliveryGroup dg
            JOIN dg.slot s
            WHERE dg.deletedAt IS NULL
              AND (:deliveryDate IS NULL OR dg.deliveryDate = :deliveryDate)
              AND (:slotCode IS NULL OR s.code = :slotCode)
              AND (:status IS NULL OR dg.status = :status)
        """
    )
    Page<DeliveryGroup> findAllForAdmin(
        @Param("deliveryDate") LocalDate deliveryDate
        , @Param("slotCode") DeliverySlotCode slotCode
        , @Param("status") DeliveryGroupStatus status
        , Pageable pageable
    );

    @Query("""
        SELECT dg
        FROM DeliveryGroup dg
        JOIN FETCH dg.slot
        WHERE dg.id = :deliveryGroupId
          AND dg.deletedAt IS NULL
    """)
    Optional<DeliveryGroup> findDetailById(
        @Param("deliveryGroupId") Long deliveryGroupId
    );

    @Query("""
        SELECT dg
        FROM DeliveryGroup dg
        JOIN FETCH dg.slot s
        WHERE dg.deletedAt IS NULL
          AND (:deliveryDate IS NULL OR dg.deliveryDate = :deliveryDate)
          AND (:slotCode IS NULL OR s.code = :slotCode)
        ORDER BY dg.deliveryDate DESC, dg.id ASC
    """)
    List<DeliveryGroup> findAllForOperations(
        @Param("deliveryDate") LocalDate deliveryDate
        , @Param("slotCode") DeliverySlotCode slotCode
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
