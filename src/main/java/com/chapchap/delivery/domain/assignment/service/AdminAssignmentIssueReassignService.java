package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueResolution;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.event.RiderAssignmentReassignedEvent;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.AssignmentConditionNotMetException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import com.chapchap.delivery.global.exception.business.DeliveryCapacityExceededException;
import com.chapchap.delivery.global.exception.business.InvalidAssignmentIssueReasonException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AdminAssignmentIssueReassignService {

    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private static final String ASSIGNMENT_ISSUE_ENTITY_TYPE =
        "DELIVERY_ASSIGNMENT_ISSUE";

    private static final String ASSIGNMENT_ISSUE_REASSIGNED_ACTION =
        "ASSIGNMENT_ISSUE_REASSIGNED";

    private final DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    private final DeliveryGroupRepository deliveryGroupRepository;
    private final DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    private final DeliveryRepository deliveryRepository;
    private final RiderRepository riderRepository;
    private final AuditHistoryRepository auditHistoryRepository;
    private final DeliveryAccessService deliveryAccessService;
    private final RiderAssignmentEligibilityService riderAssignmentEligibilityService;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AdminAssignmentIssueReassignService(
        DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository
        , DeliveryAssignmentRepository deliveryAssignmentRepository
        , DeliveryAssignmentItemRepository deliveryAssignmentItemRepository
        , DeliveryGroupRepository deliveryGroupRepository
        , DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository
        , DeliveryRepository deliveryRepository
        , RiderRepository riderRepository
        , AuditHistoryRepository auditHistoryRepository
        , DeliveryAccessService deliveryAccessService
        , RiderAssignmentEligibilityService riderAssignmentEligibilityService
        , EntityManager entityManager
        , ApplicationEventPublisher applicationEventPublisher
    ) {
        this.deliveryAssignmentIssueRepository = deliveryAssignmentIssueRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryAssignmentItemRepository = deliveryAssignmentItemRepository;
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.deliveryGroupStatusHistoryRepository = deliveryGroupStatusHistoryRepository;
        this.deliveryRepository = deliveryRepository;
        this.riderRepository = riderRepository;
        this.auditHistoryRepository = auditHistoryRepository;
        this.deliveryAccessService = deliveryAccessService;
        this.riderAssignmentEligibilityService = riderAssignmentEligibilityService;
        this.entityManager = entityManager;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public DeliveryAssignment reassignIssue(
        Long actorId
        , UserRole actorRole
        , Long issueId
        , Long newRiderId
        , String reasonCode
        , String reasonDetail
    ) {
        deliveryAccessService.validateAdminAccess(
            actorId
            , actorRole
        );

        String normalizedReasonCode =
            validateAndNormalizeReasonCode(
                reasonCode
            );

        String normalizedReasonDetail =
            validateAndNormalizeReasonDetail(
                normalizedReasonCode
                , reasonDetail
            );

        DeliveryAssignmentIssue issue =
            deliveryAssignmentIssueRepository.findByIdAndDeletedAtIsNull(
                    issueId
                )
                .orElseThrow(
                    DeliveryAssignmentNotFoundException::new
                );

        DeliveryAssignment originalAssignment =
            issue.getAssignment();

        Long originalAssignmentId =
            originalAssignment.getId();

        Long deliveryGroupId =
            originalAssignment.getDeliveryGroup()
                .getId();

        Long originalRiderId =
            originalAssignment.getRider()
                .getId();

        DeliveryGroup deliveryGroup =
            deliveryGroupRepository.findByIdForUpdate(
                    deliveryGroupId
                )
                .orElseThrow(
                    DeliveryAssignmentNotFoundException::new
                );

        validateBeforeDeliveryStart(
            deliveryGroup
        );

        List<Long> riderIds =
            new ArrayList<>(
                List.of(
                    originalRiderId
                    , newRiderId
                )
            );

        riderIds.sort(
            Comparator.naturalOrder()
        );

        List<Rider> lockedRiders =
            riderRepository.findAllByIdInForUpdate(
                riderIds
            );

        Rider newRider =
            findNewRider(
                lockedRiders
                , newRiderId
            );

        if (originalRiderId.equals(newRiderId)) {
            throw new AssignmentConditionNotMetException();
        }

        deliveryRepository.findAllByDeliveryGroupIdForUpdate(
            deliveryGroupId
        );

        List<DeliveryAssignment> assignments =
            deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(
                deliveryGroupId
            );

        List<DeliveryAssignmentItem> assignmentItems =
            deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(
                deliveryGroupId
            );

        DeliveryAssignment lockedOriginalAssignment =
            findOriginalAssignment(
                assignments
                , originalAssignmentId
            );

        validateOriginalAssignment(
            deliveryGroup
            , lockedOriginalAssignment
        );

        List<DeliveryAssignmentItem> originalItems =
            findOriginalAssignmentItems(
                assignmentItems
                , originalAssignmentId
            );

        if (originalItems.isEmpty()) {
            throw new DeliveryAssignmentStateConflictException();
        }

        validateOriginalAssignmentOwnership(
            lockedOriginalAssignment
            , assignments
            , assignmentItems
            , originalItems
        );

        validateOriginalDeliveries(
            originalItems
        );

        validateNewRiderEligibility(
            newRider
            , deliveryGroup
            , originalItems
        );

        validateNewRiderCapacity(
            newRider
            , assignments
            , assignmentItems
            , originalItems
        );

        LocalDateTime reassignedAt =
            LocalDateTime.now(KST);

        int issueUpdatedCount =
            deliveryAssignmentIssueRepository.resolveIfUnresolved(
                issueId
                , DeliveryAssignmentIssueResolution.REASSIGNED
                , actorId
                , reassignedAt
            );

        if (issueUpdatedCount == 0) {
            throw new DeliveryAssignmentStateConflictException();
        }

        int assignmentUpdatedCount =
            deliveryAssignmentRepository.reassignIfIssueReported(
                originalAssignmentId
                , DeliveryAssignmentStatus.ISSUE_REPORTED
                , DeliveryAssignmentStatus.REASSIGNED
            );

        if (assignmentUpdatedCount == 0) {
            throw new DeliveryAssignmentStateConflictException();
        }

        DeliveryAssignment newAssignment =
            deliveryAssignmentRepository.save(
                new DeliveryAssignment(
                    deliveryGroup
                    , newRider
                    , DeliveryAssignmentType.MANUAL
                    , reassignedAt
                    , actorId
                )
            );

        List<DeliveryAssignmentItem> newAssignmentItems =
            createNewAssignmentItems(
                newAssignment
                , originalItems
            );

        deliveryAssignmentItemRepository.saveAll(
            newAssignmentItems
        );

        changeDeliveryGroupToWaitingRiderIfResolved(
            deliveryGroup
            , reassignedAt
        );

        saveAuditHistory(
            issue
            , originalAssignmentId
            , originalRiderId
            , newAssignment
            , newRiderId
            , actorId
            , normalizedReasonCode
            , normalizedReasonDetail
            , reassignedAt
        );

        entityManager.refresh(
            issue
        );

        entityManager.refresh(
            lockedOriginalAssignment
        );

        applicationEventPublisher.publishEvent(
            new RiderAssignmentReassignedEvent(
                newAssignment.getId()
            )
        );

        return newAssignment;
    }

    private String validateAndNormalizeReasonCode(
        String reasonCode
    ) {
        if (
            reasonCode == null
                || reasonCode.isBlank()
        ) {
            throw new InvalidAssignmentIssueReasonException();
        }

        return reasonCode.trim();
    }

    private String validateAndNormalizeReasonDetail(
        String reasonCode
        , String reasonDetail
    ) {
        if (
            "OTHER".equals(reasonCode)
                && (
                reasonDetail == null
                    || reasonDetail.isBlank()
            )
        ) {
            throw new OtherReasonDetailRequiredException();
        }

        if (reasonDetail == null) {
            return null;
        }

        String normalized =
            reasonDetail.trim();

        return normalized.isEmpty()
            ? null
            : normalized;
    }

    private void validateBeforeDeliveryStart(
        DeliveryGroup deliveryGroup
    ) {
        if (deliveryGroup.getActualStartedAt() != null) {
            throw new DeliveryAssignmentStateConflictException();
        }
    }

    private Rider findNewRider(
        List<Rider> lockedRiders
        , Long newRiderId
    ) {
        return lockedRiders.stream()
            .filter(
                rider ->
                    rider.getId()
                        .equals(newRiderId)
            )
            .findFirst()
            .orElseThrow(
                RiderNotFoundException::new
            );
    }

    private DeliveryAssignment findOriginalAssignment(
        List<DeliveryAssignment> assignments
        , Long originalAssignmentId
    ) {
        return assignments.stream()
            .filter(
                assignment ->
                    assignment.getId()
                        .equals(originalAssignmentId)
            )
            .findFirst()
            .orElseThrow(
                DeliveryAssignmentNotFoundException::new
            );
    }

    private void validateOriginalAssignment(
        DeliveryGroup deliveryGroup
        , DeliveryAssignment assignment
    ) {
        if (!deliveryGroup.isIssueReview()) {
            throw new DeliveryAssignmentStateConflictException();
        }

        if (
            assignment.getStatus()
                != DeliveryAssignmentStatus.ISSUE_REPORTED
        ) {
            throw new DeliveryAssignmentStateConflictException();
        }
    }

    private List<DeliveryAssignmentItem> findOriginalAssignmentItems(
        List<DeliveryAssignmentItem> assignmentItems
        , Long originalAssignmentId
    ) {
        return assignmentItems.stream()
            .filter(
                assignmentItem ->
                    assignmentItem.getAssignment()
                        .getId()
                        .equals(originalAssignmentId)
            )
            .toList();
    }

    private void validateOriginalDeliveries(
        List<DeliveryAssignmentItem> originalItems
    ) {
        for (DeliveryAssignmentItem assignmentItem : originalItems) {
            if (
                assignmentItem.getDelivery()
                    .getStatus()
                    != DeliveryStatus.READY
            ) {
                throw new DeliveryAssignmentStateConflictException();
            }
        }
    }

    private void validateOriginalAssignmentOwnership(
        DeliveryAssignment originalAssignment
        , List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> assignmentItems
        , List<DeliveryAssignmentItem> originalItems
    ) {
        List<Long> activeAssignmentIds =
            assignments.stream()
                .filter(
                    assignment ->
                        assignment.getStatus()
                            .isActive()
                )
                .map(DeliveryAssignment::getId)
                .toList();

        Long originalAssignmentId =
            originalAssignment.getId();

        for (DeliveryAssignmentItem originalItem : originalItems) {
            Long deliveryId =
                originalItem.getDelivery()
                    .getId();

            List<DeliveryAssignmentItem> activeOwnerItems =
                assignmentItems.stream()
                    .filter(
                        assignmentItem ->
                            activeAssignmentIds.contains(
                                assignmentItem.getAssignment()
                                    .getId()
                            )
                    )
                    .filter(
                        assignmentItem ->
                            deliveryId.equals(
                                assignmentItem.getDelivery()
                                    .getId()
                            )
                    )
                    .toList();

            if (
                activeOwnerItems.size() != 1
                    || !originalAssignmentId.equals(
                    activeOwnerItems.getFirst()
                        .getAssignment()
                        .getId()
                )
            ) {
                throw new DeliveryAssignmentStateConflictException();
            }
        }
    }

    private void validateNewRiderEligibility(
        Rider newRider
        , DeliveryGroup deliveryGroup
        , List<DeliveryAssignmentItem> originalItems
    ) {
        for (DeliveryAssignmentItem assignmentItem : originalItems) {
            Delivery delivery =
                assignmentItem.getDelivery();

            boolean eligible =
                riderAssignmentEligibilityService.isEligible(
                    newRider
                    , deliveryGroup.getDeliveryDate()
                    , deliveryGroup.getSlot()
                        .getCode()
                    , delivery.getDeliveryAreaCode()
                );

            if (!eligible) {
                throw new AssignmentConditionNotMetException();
            }
        }
    }

    private void validateNewRiderCapacity(
        Rider newRider
        , List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> assignmentItems
        , List<DeliveryAssignmentItem> originalItems
    ) {
        RiderAssignmentLoad riderLoad =
            createCurrentRiderLoad(
                newRider
                , assignments
                , assignmentItems
            );

        for (DeliveryAssignmentItem originalItem : originalItems) {
            Integer lunchboxQuantity =
                originalItem.getDelivery()
                    .getLunchboxQuantity();

            if (!riderLoad.canAssign(
                lunchboxQuantity
            )) {
                throw new DeliveryCapacityExceededException();
            }

            riderLoad.add(
                lunchboxQuantity
            );
        }
    }

    private RiderAssignmentLoad createCurrentRiderLoad(
        Rider rider
        , List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> assignmentItems
    ) {
        List<Long> activeAssignmentIds =
            assignments.stream()
                .filter(
                    assignment ->
                        assignment.getStatus()
                            .isActive()
                )
                .filter(
                    assignment ->
                        assignment.getRider()
                            .getId()
                            .equals(rider.getId())
                )
                .map(
                    DeliveryAssignment::getId
                )
                .toList();

        RiderAssignmentLoad riderLoad =
            new RiderAssignmentLoad(
                0
                , 0
            );

        for (DeliveryAssignmentItem assignmentItem : assignmentItems) {
            if (
                !activeAssignmentIds.contains(
                    assignmentItem.getAssignment()
                        .getId()
                )
            ) {
                continue;
            }

            riderLoad.add(
                assignmentItem.getDelivery()
                    .getLunchboxQuantity()
            );
        }

        return riderLoad;
    }

    private List<DeliveryAssignmentItem> createNewAssignmentItems(
        DeliveryAssignment newAssignment
        , List<DeliveryAssignmentItem> originalItems
    ) {
        List<DeliveryAssignmentItem> newAssignmentItems =
            new ArrayList<>();

        for (DeliveryAssignmentItem originalItem : originalItems) {
            newAssignmentItems.add(
                new DeliveryAssignmentItem(
                    newAssignment
                    , originalItem.getDelivery()
                )
            );
        }

        return newAssignmentItems;
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
        , Long originalAssignmentId
        , Long originalRiderId
        , DeliveryAssignment newAssignment
        , Long newRiderId
        , Long actorId
        , String reasonCode
        , String reasonDetail
        , LocalDateTime occurredAt
    ) {
        AuditHistory auditHistory =
            AuditHistory.record(
                ASSIGNMENT_ISSUE_ENTITY_TYPE
                , issue.getId()
                , ASSIGNMENT_ISSUE_REASSIGNED_ACTION
                , actorId
                , AuditActorType.ADMIN
                , reasonCode
                , reasonDetail
                , createBeforeValueJson(
                    originalAssignmentId
                    , originalRiderId
                )
                , createAfterValueJson(
                    originalAssignmentId
                    , newAssignment.getId()
                    , newRiderId
                )
                , occurredAt
            );

        auditHistoryRepository.save(
            auditHistory
        );
    }

    private String createBeforeValueJson(
        Long originalAssignmentId
        , Long originalRiderId
    ) {
        return """
            {"resolution":null,"assignmentId":%d,"assignmentStatus":"ISSUE_REPORTED","riderId":%d}
            """
            .formatted(
                originalAssignmentId
                , originalRiderId
            )
            .trim();
    }

    private String createAfterValueJson(
        Long originalAssignmentId
        , Long newAssignmentId
        , Long newRiderId
    ) {
        return """
            {"resolution":"REASSIGNED","previousAssignmentId":%d,"previousAssignmentStatus":"REASSIGNED","newAssignmentId":%d,"newAssignmentStatus":"ASSIGNED","newRiderId":%d}
            """
            .formatted(
                originalAssignmentId
                , newAssignmentId
                , newRiderId
            )
            .trim();
    }
}
