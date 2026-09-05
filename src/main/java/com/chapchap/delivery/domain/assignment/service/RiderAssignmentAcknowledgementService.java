package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentAcknowledgementTime;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
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
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class RiderAssignmentAcknowledgementService {
    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryGroupRepository deliveryGroupRepository;
    private final DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    private final DeliveryAccessService deliveryAccessService;
    private final EntityManager entityManager;

    public RiderAssignmentAcknowledgementService(
        DeliveryAssignmentRepository deliveryAssignmentRepository
        , DeliveryGroupRepository deliveryGroupRepository
        , DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository
        , DeliveryAccessService deliveryAccessService
        , EntityManager entityManager
    ) {
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.deliveryGroupStatusHistoryRepository = deliveryGroupStatusHistoryRepository;
        this.deliveryAccessService = deliveryAccessService;
        this.entityManager = entityManager;
    }

    @Transactional
    public DeliveryAssignment acknowledge(
        Long authUserId
        , Long assignmentId
    ) {
        validateRiderAccess(
            authUserId
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

        if (assignment.isAcknowledged()) {
            return assignment;
        }

        if (!assignment.isAssigned()) {
            throw new DeliveryAssignmentStateConflictException();
        }

        LocalDateTime acknowledgedAt =
            LocalDateTime.now(KST);

        validateAcknowledgementTime(
            deliveryGroup
            , acknowledgedAt
        );

        int updatedCount =
            deliveryAssignmentRepository.acknowledgeIfAssigned(
                assignmentId
                , authUserId
                , DeliveryAssignmentStatus.ASSIGNED
                , DeliveryAssignmentStatus.ACKNOWLEDGED
                , acknowledgedAt
            );

        entityManager.refresh(
            assignment
        );

        if (updatedCount == 0) {
            if (assignment.isAcknowledged()) {
                return assignment;
            }

            throw new DeliveryAssignmentStateConflictException();
        }

        updateDeliveryGroupIfReady(
            deliveryGroup
            , acknowledgedAt
        );

        return assignment;
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

    private void validateAcknowledgementTime(
        DeliveryGroup deliveryGroup
        , LocalDateTime now
    ) {
        if (!deliveryGroup.getDeliveryDate().equals(now.toLocalDate())) {
            throw new DeliveryAssignmentStateConflictException();
        }

        LocalTime acknowledgementStart =
            DeliveryAssignmentAcknowledgementTime.getStart(
                deliveryGroup.getSlot()
                    .getCode()
            );

        if (now.toLocalTime().isBefore(acknowledgementStart)) {
            throw new DeliveryAssignmentStateConflictException();
        }
    }

    private void updateDeliveryGroupIfReady(
        DeliveryGroup deliveryGroup
        , LocalDateTime changedAt
    ) {
        List<DeliveryAssignment> assignments =
            deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(
                deliveryGroup.getId()
            );

        boolean allAcknowledged =
            assignments.stream()
                .filter(
                    assignment ->
                        assignment.getStatus()
                            .isActive()
                )
                .allMatch(
                    DeliveryAssignment::isAcknowledged
                );

        if (!allAcknowledged) {
            return;
        }

        if (!deliveryGroup.isWaitingRider()) {
            return;
        }

        deliveryGroup.readyToConfirm();

        deliveryGroupStatusHistoryRepository.save(
            new DeliveryGroupStatusHistory(
                deliveryGroup
                , DeliveryGroupStatus.WAITING_RIDER
                , DeliveryGroupStatus.READY_TO_CONFIRM
                , null
                , DeliveryGroupChangedByType.SYSTEM
                , changedAt
            )
        );
    }
}