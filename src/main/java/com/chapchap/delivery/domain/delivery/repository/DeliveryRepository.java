package com.chapchap.delivery.domain.delivery.repository;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByDeliveryPublicId(String deliveryPublicId);

    @Query(
        value = """
            SELECT d
            FROM Delivery d
            JOIN FETCH d.deliveryGroup g
            JOIN FETCH g.slot s
            WHERE d.customerId = :customerId
              AND g.deletedAt IS NULL
              AND (:dateFrom IS NULL OR g.deliveryDate >= :dateFrom)
              AND (:dateTo IS NULL OR g.deliveryDate <= :dateTo)
              AND (:slotCode IS NULL OR s.code = :slotCode)
              AND (:status IS NULL OR d.status = :status)
        """
        , countQuery = """
            SELECT COUNT(d)
            FROM Delivery d
            JOIN d.deliveryGroup g
            JOIN g.slot s
            WHERE d.customerId = :customerId
              AND g.deletedAt IS NULL
              AND (:dateFrom IS NULL OR g.deliveryDate >= :dateFrom)
              AND (:dateTo IS NULL OR g.deliveryDate <= :dateTo)
              AND (:slotCode IS NULL OR s.code = :slotCode)
              AND (:status IS NULL OR d.status = :status)
        """
    )
    Page<Delivery> findAllForCustomer(
        @Param("customerId") Long customerId
        , @Param("dateFrom") LocalDate dateFrom
        , @Param("dateTo") LocalDate dateTo
        , @Param("slotCode") DeliverySlotCode slotCode
        , @Param("status") DeliveryStatus status
        , Pageable pageable
    );

    @Query("""
        SELECT d
        FROM Delivery d
        JOIN FETCH d.deliveryGroup g
        JOIN FETCH g.slot
        WHERE d.deliveryPublicId = :deliveryPublicId
          AND g.deletedAt IS NULL
    """)
    Optional<Delivery> findDetailByDeliveryPublicId(
        @Param("deliveryPublicId") String deliveryPublicId
    );

    @Query("""
        SELECT d
        FROM Delivery d
        JOIN FETCH d.deliveryGroup g
        JOIN FETCH g.slot
        WHERE g.id IN :deliveryGroupIds
        ORDER BY d.id ASC
    """)
    List<Delivery> findAllByDeliveryGroupIdIn(
        @Param("deliveryGroupIds") List<Long> deliveryGroupIds
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE Delivery d
        SET d.status = :nextStatus,
            d.deliveryVersion = d.deliveryVersion + 1
        WHERE d.id = :deliveryId
          AND d.status = :expectedStatus
    """)
    int transitionStatus(
        @Param("deliveryId") Long deliveryId
        , @Param("expectedStatus") DeliveryStatus expectedStatus
        , @Param("nextStatus") DeliveryStatus nextStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d
        FROM Delivery d
        WHERE d.deliveryPublicId = :deliveryPublicId
    """)
    Optional<Delivery> findByDeliveryPublicIdForUpdate(
        @Param("deliveryPublicId") String deliveryPublicId
    );

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d
        FROM Delivery d
        JOIN FETCH d.deliveryGroup g
        JOIN FETCH g.slot s
        WHERE g.deliveryDate = :deliveryDate
          AND s.code = :slotCode
          AND d.status IN :statuses
        ORDER BY d.id ASC
    """)
    List<Delivery> findUnfinishedByDeliveryDateAndSlotForUpdate(
        @Param("deliveryDate") LocalDate deliveryDate
        , @Param("slotCode") DeliverySlotCode slotCode
        , @Param("statuses") List<DeliveryStatus> statuses
    );

    @Query("""
        SELECT d
        FROM Delivery d
        JOIN FETCH d.deliveryGroup g
        JOIN FETCH g.slot s
        WHERE g.deliveryDate = :deliveryDate
          AND s.code = :slotCode
          AND d.status IN :statuses
        ORDER BY d.id ASC
    """)
    List<Delivery> findUnresolvedByDeliveryDateAndSlot(
        @Param("deliveryDate") LocalDate deliveryDate
        , @Param("slotCode") DeliverySlotCode slotCode
        , @Param("statuses") List<DeliveryStatus> statuses
    );
}
