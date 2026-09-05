package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueResolution;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import com.chapchap.delivery.global.exception.business.InvalidAssignmentIssueReasonException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class AdminAssignmentIssueRejectService {

    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private static final String ASSIGNMENT_ISSUE_ENTITY_TYPE =
        "DELIVERY_ASSIGNMENT_ISSUE";

    private static final String ASSIGNMENT_ISSUE_REJECTED_ACTION =
        "ASSIGNMENT_ISSUE_REJECTED";

    private final DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryGroupRepository deliveryGroupRepository;
    private final DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    private final AuditHistoryRepository auditHistoryRepository;
    private final DeliveryAccessService deliveryAccessService;
    private final EntityManager entityManager;

    public AdminAssignmentIssueRejectService(
        DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository
        , DeliveryAssignmentRepository deliveryAssignmentRepository
        , DeliveryGroupRepository deliveryGroupRepository
        , DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository
        , AuditHistoryRepository auditHistoryRepository
        , DeliveryAccessService deliveryAccessService
        , EntityManager entityManager
    ) {
        this.deliveryAssignmentIssueRepository = deliveryAssignmentIssueRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.deliveryGroupStatusHistoryRepository = deliveryGroupStatusHistoryRepository;
        this.auditHistoryRepository = auditHistoryRepository;
        this.deliveryAccessService = deliveryAccessService;
        this.entityManager = entityManager;
    }

    @Transactional
    public DeliveryAssignmentIssue rejectIssue(
        Long actorId
        , UserRole actorRole
        , Long issueId
        , String reasonDetail
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        String normalizedReasonDetail =
            validateAndNormalizeReasonDetail(
                reasonDetail
            );

        DeliveryAssignmentIssue issue =
            deliveryAssignmentIssueRepository.findByIdAndDeletedAtIsNull(
                    issueId
                )
                .orElseThrow(
                    DeliveryAssignmentNotFoundException::new
                );

        DeliveryAssignment assignment =
            issue.getAssignment();

        Long deliveryGroupId =
            assignment.getDeliveryGroup()
                .getId();

        DeliveryGroup deliveryGroup =
            deliveryGroupRepository.findByIdForUpdate(
                    deliveryGroupId
                )
                .orElseThrow(
                    DeliveryAssignmentNotFoundException::new
                );

        LocalDateTime resolvedAt =
            LocalDateTime.now(KST);

        int resolvedCount =
            deliveryAssignmentIssueRepository.resolveIfUnresolved(
                issueId
                , DeliveryAssignmentIssueResolution.REJECTED
                , actorId
                , resolvedAt
            );

        if (resolvedCount == 0) {
            throw new DeliveryAssignmentStateConflictException();
        }

        int assignmentUpdatedCount =
            deliveryAssignmentRepository.rejectIssueIfReported(
                assignment.getId()
                , DeliveryAssignmentStatus.ISSUE_REPORTED
                , DeliveryAssignmentStatus.ASSIGNED
            );

        if (assignmentUpdatedCount == 0) {
            throw new DeliveryAssignmentStateConflictException();
        }

        changeDeliveryGroupToWaitingRiderIfResolved(
            deliveryGroup
            , resolvedAt
        );

        saveAuditHistory(
            issue
            , actorId
            , normalizedReasonDetail
            , resolvedAt
        );

        entityManager.refresh(
            issue
        );

        entityManager.refresh(
            assignment
        );

        return issue;
    }

    private String validateAndNormalizeReasonDetail(
        String reasonDetail
    ) {
        if (
            reasonDetail == null
                || reasonDetail.isBlank()
        ) {
            throw new InvalidAssignmentIssueReasonException();
        }

        return reasonDetail.trim();
    }

    private void changeDeliveryGroupToWaitingRiderIfResolved(
        DeliveryGroup deliveryGroup
        , LocalDateTime changedAt
    ) {
        long unresolvedIssueCount =
            deliveryAssignmentIssueRepository.countUnresolvedByDeliveryGroupId(
                deliveryGroup.getId()
            );

        if (unresolvedIssueCount > 0) {
            return;
        }

        if (!deliveryGroup.isIssueReview()) {
            return;
        }

        DeliveryGroupStatus fromStatus =
            deliveryGroup.getStatus();

        deliveryGroup.returnToWaitingRider();

        deliveryGroupStatusHistoryRepository.save(
            new DeliveryGroupStatusHistory(
                deliveryGroup
                , fromStatus
                , DeliveryGroupStatus.WAITING_RIDER
                , null
                , DeliveryGroupChangedByType.SYSTEM
                , changedAt
            )
        );
    }

    private void saveAuditHistory(
        DeliveryAssignmentIssue issue
        , Long actorId
        , String reasonDetail
        , LocalDateTime occurredAt
    ) {
        AuditHistory auditHistory =
            AuditHistory.record(
                ASSIGNMENT_ISSUE_ENTITY_TYPE
                , issue.getId()
                , ASSIGNMENT_ISSUE_REJECTED_ACTION
                , actorId
                , AuditActorType.ADMIN
                , DeliveryAssignmentIssueResolution.REJECTED.name()
                , reasonDetail
                , createBeforeValueJson()
                , createAfterValueJson()
                , occurredAt
            );

        auditHistoryRepository.save(
            auditHistory
        );
    }

    private String createBeforeValueJson() {
        return """
            {"resolution":null,"assignmentStatus":"ISSUE_REPORTED"}
            """
            .trim();
    }

    private String createAfterValueJson() {
        return """
            {"resolution":"REJECTED","assignmentStatus":"ASSIGNED"}
            """
            .trim();
    }
}