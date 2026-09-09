package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueCode;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import com.chapchap.delivery.global.exception.business.InvalidAssignmentIssueReasonException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class RiderAssignmentIssueService {
    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository;
    private final DeliveryGroupRepository deliveryGroupRepository;
    private final DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    private final DeliveryAccessService deliveryAccessService;
    private final EntityManager entityManager;

    public RiderAssignmentIssueService(
        DeliveryAssignmentRepository deliveryAssignmentRepository
        , DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository
        , DeliveryGroupRepository deliveryGroupRepository
        , DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository
        , DeliveryAccessService deliveryAccessService
        , EntityManager entityManager
    ) {
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryAssignmentIssueRepository = deliveryAssignmentIssueRepository;
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.deliveryGroupStatusHistoryRepository = deliveryGroupStatusHistoryRepository;
        this.deliveryAccessService = deliveryAccessService;
        this.entityManager = entityManager;
    }

    @Transactional
    public DeliveryAssignmentIssue reportIssue(
        Long authUserId
        , Long assignmentId
        , DeliveryAssignmentIssueCode issueCode
        , String issueDetail
    ) {
        validateRiderAccess(
            authUserId
        );

        validateIssueReason(
            issueCode
            , issueDetail
        );

        DeliveryAssignment assignment =
            deliveryAssignmentRepository.findMineById(
                    assignmentId
                    , authUserId
                )
                .orElseThrow(
                    DeliveryAssignmentNotFoundException::new
                );

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

        validateRiderActive(
            assignment
        );

        if (!assignment.canReportIssue()) {
            throw new DeliveryAssignmentStateConflictException();
        }

        LocalDateTime reportedAt =
            LocalDateTime.now(KST);

        int updatedCount =
            deliveryAssignmentRepository.reportIssueIfReportable(
                assignmentId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , DeliveryAssignmentStatus.ISSUE_REPORTED
            );

        entityManager.refresh(
            assignment
        );

        if (updatedCount == 0) {
            throw new DeliveryAssignmentStateConflictException();
        }

        DeliveryAssignmentIssue issue =
            new DeliveryAssignmentIssue(
                assignment
                , issueCode
                , issueDetail
                , authUserId
                , reportedAt
            );

        changeDeliveryGroupToIssueReview(
            deliveryGroup
            , reportedAt
        );

        return deliveryAssignmentIssueRepository.save(
            issue
        );
    }

    private void validateRiderAccess(Long authUserId) {
        if (!deliveryAccessService.isRiderAccessAllowed(authUserId)) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    private void validateRiderActive(
        DeliveryAssignment assignment
    ) {
        if (!Boolean.TRUE.equals(
            assignment.getRider()
                .getIsDeliveryActive()
        )) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    private void validateIssueReason(
        DeliveryAssignmentIssueCode issueCode
        , String issueDetail
    ) {
        if (issueCode == null) {
            throw new InvalidAssignmentIssueReasonException();
        }

        if (
            issueCode == DeliveryAssignmentIssueCode.OTHER
                && (
                issueDetail == null
                    || issueDetail.isBlank()
            )
        ) {
            throw new OtherReasonDetailRequiredException();
        }
    }

    private void changeDeliveryGroupToIssueReview(
        DeliveryGroup deliveryGroup
        , LocalDateTime changedAt
    ) {
        if (deliveryGroup.isIssueReview()) {
            return;
        }

        DeliveryGroupStatus fromStatus =
            deliveryGroup.getStatus();

        deliveryGroup.issueReview();

        deliveryGroupStatusHistoryRepository.save(
            new DeliveryGroupStatusHistory(
                deliveryGroup
                , fromStatus
                , DeliveryGroupStatus.ISSUE_REVIEW
                , null
                , DeliveryGroupChangedByType.SYSTEM
                , changedAt
            )
        );
    }
}