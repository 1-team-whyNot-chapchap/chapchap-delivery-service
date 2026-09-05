package com.chapchap.delivery.domain.assignment.repository;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueResolution;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DeliveryAssignmentIssueRepository
    extends JpaRepository<DeliveryAssignmentIssue, Long> {

    Optional<DeliveryAssignmentIssue> findByIdAndDeletedAtIsNull(
        Long issueId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE DeliveryAssignmentIssue dai
        SET dai.resolution = :resolution
            , dai.resolvedBy = :resolvedBy
            , dai.resolvedAt = :resolvedAt
        WHERE dai.id = :issueId
            AND dai.deletedAt IS NULL
            AND dai.resolution IS NULL
    """)
    int resolveIfUnresolved(
        @Param("issueId") Long issueId
        , @Param("resolution") DeliveryAssignmentIssueResolution resolution
        , @Param("resolvedBy") Long resolvedBy
        , @Param("resolvedAt") LocalDateTime resolvedAt
    );

    @Query("""
        SELECT COUNT(dai.id)
        FROM DeliveryAssignmentIssue dai
        JOIN dai.assignment da
        WHERE da.deliveryGroup.id = :deliveryGroupId
            AND dai.resolution IS NULL
            AND dai.deletedAt IS NULL
            AND da.deletedAt IS NULL
    """)
    long countUnresolvedByDeliveryGroupId(
        @Param("deliveryGroupId") Long deliveryGroupId
    );
}