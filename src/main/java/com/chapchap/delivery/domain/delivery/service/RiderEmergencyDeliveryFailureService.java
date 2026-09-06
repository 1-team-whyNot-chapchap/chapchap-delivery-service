package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.request.RiderEmergencyDeliveryFailureRequest;
import com.chapchap.delivery.domain.delivery.response.RiderEmergencyDeliveryFailureResponse;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryFailureReasonException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import com.chapchap.delivery.global.kafka.producer.DeliveryEventRequestPublisher;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderEmergencyDeliveryFailureService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryAccessService accessService;
    private final DeliveryGroupRepository groupRepository;
    private final RiderRepository riderRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentItemRepository itemRepository;
    private final DeliveryFailureRepository failureRepository;
    private final DeliveryStatusHistoryRepository historyRepository;
    private final DeliveryExecutionSupport executionSupport;
    private final DeliveryRefundReasonResolver refundReasonResolver;
    private final DeliveryEventRequestPublisher eventPublisher;
    private final EntityManager entityManager;

    public RiderEmergencyDeliveryFailureService(
        DeliveryAccessService accessService
        , DeliveryGroupRepository groupRepository
        , RiderRepository riderRepository
        , DeliveryRepository deliveryRepository
        , DeliveryAssignmentRepository assignmentRepository
        , DeliveryAssignmentItemRepository itemRepository
        , DeliveryFailureRepository failureRepository
        , DeliveryStatusHistoryRepository historyRepository
        , DeliveryExecutionSupport executionSupport
        , DeliveryRefundReasonResolver refundReasonResolver
        , DeliveryEventRequestPublisher eventPublisher
        , EntityManager entityManager
    ) {
        this.accessService = accessService;
        this.groupRepository = groupRepository;
        this.riderRepository = riderRepository;
        this.deliveryRepository = deliveryRepository;
        this.assignmentRepository = assignmentRepository;
        this.itemRepository = itemRepository;
        this.failureRepository = failureRepository;
        this.historyRepository = historyRepository;
        this.executionSupport = executionSupport;
        this.refundReasonResolver = refundReasonResolver;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }

    @Transactional
    public RiderEmergencyDeliveryFailureResponse failRemaining(
        Long authUserId
        , Long assignmentId
        , RiderEmergencyDeliveryFailureRequest request
    ) {
        accessService.validateRiderAccess(authUserId, UserRole.RIDER);
        validateRequest(request);
        DeliveryAssignment reference = assignmentRepository.findMineById(
            assignmentId
            , authUserId
        ).orElseThrow(DeliveryAccessForbiddenException::new);
        DeliveryGroup group = groupRepository.findByIdForUpdate(
            reference.getDeliveryGroup().getId()
        ).orElseThrow(DeliveryNotFoundException::new);
        if (group.getStatus() != DeliveryGroupStatus.IN_PROGRESS) {
            throw new DeliveryStateConflictException();
        }
        Rider rider = riderRepository.findByAuthUserIdForUpdate(authUserId)
            .orElseThrow(RiderNotFoundException::new);
        if (!Boolean.TRUE.equals(rider.getIsDeliveryActive())) {
            throw new DeliveryAccessForbiddenException();
        }
        List<Delivery> deliveries = deliveryRepository.findAllByDeliveryGroupIdForUpdate(
            group.getId()
        );
        List<DeliveryAssignment> assignments =
            assignmentRepository.findAllByDeliveryGroupIdForUpdate(group.getId());
        List<DeliveryAssignmentItem> items = itemRepository.findAllByDeliveryGroupIdForUpdate(
            group.getId()
        );
        DeliveryAssignment assignment = assignments.stream()
            .filter(candidate -> candidate.getId().equals(assignmentId))
            .filter(candidate -> candidate.getRider().getId().equals(rider.getId()))
            .filter(candidate -> candidate.getStatus() == DeliveryAssignmentStatus.CONFIRMED)
            .findFirst()
            .orElseThrow(DeliveryAccessForbiddenException::new);

        LocalDateTime failedAt = LocalDateTime.now(KST);
        List<String> failedIds = new ArrayList<>();
        for (DeliveryAssignmentItem item : items) {
            if (!item.getAssignment().getId().equals(assignment.getId())) {
                continue;
            }
            Delivery delivery = item.getDelivery();
            if (delivery.isFinished()) {
                continue;
            }
            DeliveryStatus previousStatus = delivery.getStatus();
            if (
                previousStatus != DeliveryStatus.READY
                    && previousStatus != DeliveryStatus.DELIVERING
            ) {
                continue;
            }
            if (
                deliveryRepository.transitionStatus(
                    delivery.getId()
                    , previousStatus
                    , DeliveryStatus.FAILED
                ) != 1
            ) {
                throw new DeliveryStateConflictException();
            }
            entityManager.refresh(delivery);
            DeliveryFailureStage stage = previousStatus == DeliveryStatus.READY
                ? DeliveryFailureStage.BEFORE_DEPARTURE
                : DeliveryFailureStage.DURING_DELIVERY;
            failureRepository.save(
                new DeliveryFailure(
                    delivery
                    , stage
                    , request.failureCode()
                    , request.failureDetail()
                    , null
                    , null
                    , request.itemRecovered()
                    , toLocal(request.recoveredAt())
                    , authUserId
                    , failedAt
                )
            );
            historyRepository.save(
                new DeliveryStatusHistory(
                    delivery
                    , previousStatus
                    , DeliveryStatus.FAILED
                    , authUserId
                    , DeliveryChangedByType.RIDER
                    , failedAt
                )
            );
            eventPublisher.publishStateChanged("DELIVERY_FAILED", delivery, failedAt);
            eventPublisher.publishRefundConfirmed(
                delivery
                , refundReasonResolver.resolveFailure(request.failureCode())
                , failedAt
            );
            failedIds.add(delivery.getDeliveryPublicId());
        }
        executionSupport.recalculateGroup(group, deliveries, failedAt);
        return new RiderEmergencyDeliveryFailureResponse(
            assignmentId
            , failedIds.size()
            , List.copyOf(failedIds)
        );
    }

    private void validateRequest(RiderEmergencyDeliveryFailureRequest request) {
        if (
            request.failureCode() != DeliveryFailureCode.RIDER_UNAVAILABLE
                && request.failureCode() != DeliveryFailureCode.RIDER_ACCIDENT
                && request.failureCode() != DeliveryFailureCode.VEHICLE_ISSUE
                && request.failureCode() != DeliveryFailureCode.EMERGENCY
                && request.failureCode() != DeliveryFailureCode.OTHER
        ) {
            throw new InvalidDeliveryFailureReasonException();
        }
        if (request.failureCode().requiresDetail()
            && (request.failureDetail() == null || request.failureDetail().isBlank())) {
            throw new InvalidDeliveryFailureReasonException();
        }
        if (Boolean.TRUE.equals(request.itemRecovered()) != (request.recoveredAt() != null)) {
            throw new InvalidDeliveryFailureReasonException();
        }
    }

    private LocalDateTime toLocal(java.time.OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KST).toLocalDateTime();
    }
}
