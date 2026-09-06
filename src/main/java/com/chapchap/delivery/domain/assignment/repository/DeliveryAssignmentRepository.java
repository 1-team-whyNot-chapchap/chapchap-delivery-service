package com.chapchap.delivery.domain.assignment.repository;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository
    extends JpaRepository<DeliveryAssignment, Long> {

    Optional<DeliveryAssignment> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
        SELECT da
        FROM DeliveryAssignment da
        JOIN FETCH da.rider
        WHERE da.deliveryGroup.id IN :deliveryGroupIds
          AND da.deletedAt IS NULL
        ORDER BY da.id ASC
    """)
    List<DeliveryAssignment> findAllByDeliveryGroupIdIn(
        @Param("deliveryGroupIds") List<Long> deliveryGroupIds
    );

    @Query("""
        SELECT DISTINCT da
        FROM DeliveryAssignmentItem dai
        JOIN dai.assignment da
        JOIN FETCH da.rider
        WHERE dai.delivery.id = :deliveryId
          AND dai.deletedAt IS NULL
          AND da.deletedAt IS NULL
        ORDER BY da.id ASC
    """)
    List<DeliveryAssignment> findAllByDeliveryId(
        @Param("deliveryId") Long deliveryId
    );

    @Query("""
        SELECT DISTINCT da.rider.id
        FROM DeliveryAssignment da
        WHERE da.deliveryGroup.id = :deliveryGroupId
            AND da.deletedAt IS NULL
            AND da.status <> :reassignedStatus
        ORDER BY da.rider.id ASC
    """)
    List<Long> findActiveRiderIdsByDeliveryGroupId(
        @Param("deliveryGroupId") Long deliveryGroupId
        , @Param("reassignedStatus") DeliveryAssignmentStatus reassignedStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT da
        FROM DeliveryAssignment da
        WHERE da.deliveryGroup.id = :deliveryGroupId
            AND da.deletedAt IS NULL
        ORDER BY da.id ASC
    """)
    List<DeliveryAssignment> findAllByDeliveryGroupIdForUpdate(
        @Param("deliveryGroupId") Long deliveryGroupId
    );

    @Query("""
        SELECT da
        FROM DeliveryAssignment da
        WHERE da.id = :assignmentId
            AND da.rider.authUserId = :authUserId
            AND da.deliveryGroup.deletedAt IS NULL
            AND da.rider.deletedAt IS NULL
            AND da.deletedAt IS NULL
    """)
    Optional<DeliveryAssignment> findMineById(
        @Param("assignmentId") Long assignmentId
        , @Param("authUserId") Long authUserId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE DeliveryAssignment da
        SET da.status = :acknowledgedStatus
            , da.acknowledgedAt = :acknowledgedAt
        WHERE da.id = :assignmentId
            AND da.rider.authUserId = :authUserId
            AND da.rider.deletedAt IS NULL
            AND da.deletedAt IS NULL
            AND da.status = :assignedStatus
    """)
    int acknowledgeIfAssigned(
        @Param("assignmentId") Long assignmentId
        , @Param("authUserId") Long authUserId
        , @Param("assignedStatus") DeliveryAssignmentStatus assignedStatus
        , @Param("acknowledgedStatus") DeliveryAssignmentStatus acknowledgedStatus
        , @Param("acknowledgedAt") LocalDateTime acknowledgedAt
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE DeliveryAssignment da
        SET da.status = :issueReportedStatus
            , da.acknowledgedAt = NULL
        WHERE da.id = :assignmentId
            AND da.deletedAt IS NULL
            AND (
                da.status = :assignedStatus
                OR da.status = :acknowledgedStatus
            )
    """)
    int reportIssueIfReportable(
        @Param("assignmentId") Long assignmentId
        , @Param("assignedStatus") DeliveryAssignmentStatus assignedStatus
        , @Param("acknowledgedStatus") DeliveryAssignmentStatus acknowledgedStatus
        , @Param("issueReportedStatus") DeliveryAssignmentStatus issueReportedStatus
    );

    @Query(
        value = """
            SELECT
                da.id AS assignmentId
                , dg.id AS deliveryGroupId
                , dg.deliveryDate AS deliveryDate
                , ds.code AS deliverySlot
                , da.assignmentType AS assignmentType
                , da.status AS status
                , da.assignedAt AS assignedAt
                , da.acknowledgedAt AS acknowledgedAt
                , COUNT(d.id) AS stopCount
                , COALESCE(SUM(d.lunchboxQuantity), 0) AS lunchboxQuantity
            FROM DeliveryAssignmentItem dai
                JOIN dai.assignment da
                JOIN da.deliveryGroup dg
                JOIN dg.slot ds
                JOIN dai.delivery d
            WHERE da.rider.authUserId = :authUserId
                AND da.rider.deletedAt IS NULL
                AND dg.deletedAt IS NULL
                AND da.deletedAt IS NULL
                AND dai.deletedAt IS NULL
                AND (:deliveryDate IS NULL OR dg.deliveryDate = :deliveryDate)
                AND (:deliverySlot IS NULL OR ds.code = :deliverySlot)
                AND (:status IS NULL OR da.status = :status)
            GROUP BY
                da.id
                , dg.id
                , dg.deliveryDate
                , ds.code
                , da.assignmentType
                , da.status
                , da.assignedAt
                , da.acknowledgedAt
            ORDER BY dg.deliveryDate DESC, da.id ASC
        """
        , countQuery = """
            SELECT COUNT(DISTINCT da.id)
            FROM DeliveryAssignmentItem dai
                JOIN dai.assignment da
                JOIN da.deliveryGroup dg
                JOIN dg.slot ds
                JOIN dai.delivery d
            WHERE da.rider.authUserId = :authUserId
                AND da.rider.deletedAt IS NULL
                AND dg.deletedAt IS NULL
                AND da.deletedAt IS NULL
                AND dai.deletedAt IS NULL
                AND (:deliveryDate IS NULL OR dg.deliveryDate = :deliveryDate)
                AND (:deliverySlot IS NULL OR ds.code = :deliverySlot)
                AND (:status IS NULL OR da.status = :status)
        """
    )
    Page<RiderAssignmentListProjection> findAllMine(
        @Param("authUserId") Long authUserId
        , @Param("deliveryDate") LocalDate deliveryDate
        , @Param("deliverySlot") DeliverySlotCode deliverySlot
        , @Param("status") DeliveryAssignmentStatus status
        , Pageable pageable
    );

    @Query("""
        SELECT da.id
        FROM DeliveryAssignment da
        JOIN da.deliveryGroup dg
        JOIN dg.slot ds
        JOIN da.rider r
        WHERE dg.deliveryDate = :deliveryDate
            AND ds.code = :deliverySlot
            AND da.status = :status
            AND dg.deletedAt IS NULL
            AND da.deletedAt IS NULL
            AND r.deletedAt IS NULL
            AND r.isDeliveryActive = true
        ORDER BY da.id ASC
    """)
    List<Long> findIdsForAcknowledgementPending(
        @Param("deliveryDate") LocalDate deliveryDate
        , @Param("deliverySlot") DeliverySlotCode deliverySlot
        , @Param("status") DeliveryAssignmentStatus status
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE DeliveryAssignment da
        SET da.status = :assignedStatus
        WHERE da.id = :assignmentId
            AND da.deletedAt IS NULL
            AND da.status = :issueReportedStatus
    """)
    int rejectIssueIfReported(
        @Param("assignmentId") Long assignmentId
        , @Param("issueReportedStatus") DeliveryAssignmentStatus issueReportedStatus
        , @Param("assignedStatus") DeliveryAssignmentStatus assignedStatus
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE DeliveryAssignment da
        SET da.status = :reassignedStatus
        WHERE da.id = :assignmentId
            AND da.deletedAt IS NULL
            AND da.status = :issueReportedStatus
    """)
    int reassignIfIssueReported(
        @Param("assignmentId") Long assignmentId
        , @Param("issueReportedStatus") DeliveryAssignmentStatus issueReportedStatus
        , @Param("reassignedStatus") DeliveryAssignmentStatus reassignedStatus
    );
}
