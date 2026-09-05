package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.event.RiderAssignmentAvailableEvent;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.request.AdminManualAssignmentItemRequest;
import com.chapchap.delivery.domain.assignment.request.AdminManualAssignmentsRequest;
import com.chapchap.delivery.domain.assignment.response.ManualAssignmentsResponse;
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
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import com.chapchap.delivery.global.exception.business.DeliveryCapacityExceededException;
import com.chapchap.delivery.global.exception.business.DeliveryGroupNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryGroupStateConflictException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminManualAssignmentService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryGroupRepository deliveryGroupRepository;
    private final RiderRepository riderRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    private final DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    private final AuditHistoryRepository auditHistoryRepository;
    private final DeliveryAccessService deliveryAccessService;
    private final RiderAssignmentEligibilityService riderAssignmentEligibilityService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AdminManualAssignmentService(
        DeliveryGroupRepository deliveryGroupRepository
        , RiderRepository riderRepository
        , DeliveryRepository deliveryRepository
        , DeliveryAssignmentRepository deliveryAssignmentRepository
        , DeliveryAssignmentItemRepository deliveryAssignmentItemRepository
        , DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository
        , AuditHistoryRepository auditHistoryRepository
        , DeliveryAccessService deliveryAccessService
        , RiderAssignmentEligibilityService riderAssignmentEligibilityService
        , ApplicationEventPublisher applicationEventPublisher
    ) {
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.riderRepository = riderRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryAssignmentItemRepository = deliveryAssignmentItemRepository;
        this.deliveryGroupStatusHistoryRepository = deliveryGroupStatusHistoryRepository;
        this.auditHistoryRepository = auditHistoryRepository;
        this.deliveryAccessService = deliveryAccessService;
        this.riderAssignmentEligibilityService = riderAssignmentEligibilityService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public ManualAssignmentsResponse assign(
        Long actorId
        , UserRole actorRole
        , Long deliveryGroupId
        , AdminManualAssignmentsRequest request
    ) {
        deliveryAccessService.validateAdminAccess(actorId, actorRole);

        DeliveryGroup group = deliveryGroupRepository.findByIdForUpdate(deliveryGroupId)
            .orElseThrow(DeliveryGroupNotFoundException::new);
        if (!group.isWaitingAutoAssignment()) {
            throw new DeliveryGroupStateConflictException();
        }

        List<Long> riderIds = request.assignments().stream()
            .map(AdminManualAssignmentItemRequest::riderId)
            .distinct()
            .sorted()
            .toList();
        if (riderIds.size() != request.assignments().size()) {
            throw new DeliveryAssignmentStateConflictException();
        }
        List<Rider> riders = riderRepository.findAllByIdInForUpdate(riderIds);
        List<Delivery> deliveries = deliveryRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId);
        List<DeliveryAssignment> existingAssignments =
            deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId);
        List<DeliveryAssignmentItem> existingItems =
            deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId);

        if (riders.size() != riderIds.size() || deliveries.isEmpty()) {
            throw new AssignmentConditionNotMetException();
        }
        if (existingAssignments.stream().anyMatch(a -> a.getStatus().isActive()) || !existingItems.isEmpty()) {
            throw new DeliveryAssignmentStateConflictException();
        }

        Map<Long, Rider> riderMap = new HashMap<>();
        riders.forEach(rider -> riderMap.put(rider.getId(), rider));
        Map<String, Delivery> deliveryMap = new HashMap<>();
        deliveries.forEach(delivery -> deliveryMap.put(delivery.getDeliveryPublicId(), delivery));
        Set<String> assignedDeliveryIds = new HashSet<>();
        LocalDateTime assignedAt = LocalDateTime.now(KST);
        List<DeliveryAssignment> savedAssignments = new ArrayList<>();

        for (AdminManualAssignmentItemRequest assignmentRequest : request.assignments()) {
            Rider rider = riderMap.get(assignmentRequest.riderId());
            validateAreaExceptionReason(assignmentRequest);
            RiderAssignmentLoad load = new RiderAssignmentLoad(0, 0);
            List<Delivery> requestedDeliveries = new ArrayList<>();

            for (String deliveryPublicId : assignmentRequest.deliveryIds()) {
                Delivery delivery = deliveryMap.get(deliveryPublicId);
                if (delivery == null || delivery.getStatus() != DeliveryStatus.READY
                    || !assignedDeliveryIds.add(deliveryPublicId)) {
                    throw new DeliveryAssignmentStateConflictException();
                }

                boolean eligible = assignmentRequest.areaException()
                    ? riderAssignmentEligibilityService.isEligibleIgnoringArea(
                        rider, group.getDeliveryDate(), group.getSlot().getCode())
                    : riderAssignmentEligibilityService.isEligible(
                        rider, group.getDeliveryDate(), group.getSlot().getCode(), delivery.getDeliveryAreaCode());
                if (!eligible) {
                    throw new AssignmentConditionNotMetException();
                }
                if (!load.canAssign(delivery.getLunchboxQuantity())) {
                    throw new DeliveryCapacityExceededException();
                }
                load.add(delivery.getLunchboxQuantity());
                requestedDeliveries.add(delivery);
            }

            DeliveryAssignment assignment = deliveryAssignmentRepository.save(
                new DeliveryAssignment(group, rider, DeliveryAssignmentType.MANUAL, assignedAt, actorId));
            List<DeliveryAssignmentItem> items = requestedDeliveries.stream()
                .map(delivery -> new DeliveryAssignmentItem(assignment, delivery))
                .toList();
            deliveryAssignmentItemRepository.saveAll(items);
            savedAssignments.add(assignment);

            auditHistoryRepository.save(
                AuditHistory.record(
                    "DELIVERY_ASSIGNMENT"
                    , assignment.getId()
                    , "MANUAL_ASSIGNMENT_CREATED"
                    , actorId
                    , AuditActorType.ADMIN
                    , assignmentRequest.areaException()
                        ? assignmentRequest.reasonCode().trim()
                        : null
                    , assignmentRequest.areaException()
                        ? assignmentRequest.reasonDetail().trim()
                        : null
                    , null
                    , null
                    , assignedAt
                )
            );
        }

        if (assignedDeliveryIds.size() != deliveries.size()) {
            throw new DeliveryAssignmentStateConflictException();
        }

        group.completeManualAssignment();
        deliveryGroupStatusHistoryRepository.save(new DeliveryGroupStatusHistory(
            group
            , DeliveryGroupStatus.WAITING_ASSIGNMENT
            , DeliveryGroupStatus.WAITING_RIDER
            , actorId
            , DeliveryGroupChangedByType.ADMIN
            , assignedAt
        ));

        savedAssignments.forEach(assignment ->
            applicationEventPublisher.publishEvent(new RiderAssignmentAvailableEvent(assignment.getId())));

        return new ManualAssignmentsResponse(
            deliveryGroupId
            , DeliveryGroupStatus.WAITING_RIDER
            , savedAssignments.stream().map(DeliveryAssignment::getId).toList()
        );
    }

    private void validateAreaExceptionReason(AdminManualAssignmentItemRequest request) {
        if (!request.areaException()) {
            return;
        }
        if (request.reasonCode() == null || request.reasonCode().isBlank()
            || request.reasonDetail() == null || request.reasonDetail().isBlank()) {
            throw new OtherReasonDetailRequiredException();
        }
    }
}
