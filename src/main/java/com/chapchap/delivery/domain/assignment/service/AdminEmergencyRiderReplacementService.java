package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentType;
import com.chapchap.delivery.domain.assignment.constant.EmergencyRiderReplacementReason;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.event.RiderAssignmentReassignedEvent;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.request.AdminEmergencyRiderReplacementRequest;
import com.chapchap.delivery.domain.assignment.response.AdminEmergencyRiderReplacementResponse;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.AssignmentConditionNotMetException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentStateConflictException;
import com.chapchap.delivery.global.exception.business.DeliveryCapacityExceededException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminEmergencyRiderReplacementService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryAccessService accessService;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentItemRepository itemRepository;
    private final DeliveryGroupRepository groupRepository;
    private final DeliveryRepository deliveryRepository;
    private final RiderRepository riderRepository;
    private final RiderAssignmentEligibilityService eligibilityService;
    private final AuditHistoryRepository auditRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public AdminEmergencyRiderReplacementService(
        DeliveryAccessService accessService
        , DeliveryAssignmentRepository assignmentRepository
        , DeliveryAssignmentItemRepository itemRepository
        , DeliveryGroupRepository groupRepository
        , DeliveryRepository deliveryRepository
        , RiderRepository riderRepository
        , RiderAssignmentEligibilityService eligibilityService
        , AuditHistoryRepository auditRepository
        , EntityManager entityManager
        , ApplicationEventPublisher eventPublisher
    ) {
        this.accessService = accessService;
        this.assignmentRepository = assignmentRepository;
        this.itemRepository = itemRepository;
        this.groupRepository = groupRepository;
        this.deliveryRepository = deliveryRepository;
        this.riderRepository = riderRepository;
        this.eligibilityService = eligibilityService;
        this.auditRepository = auditRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AdminEmergencyRiderReplacementResponse replace(
        Long adminId
        , UserRole role
        , Long assignmentId
        , AdminEmergencyRiderReplacementRequest request
    ) {
        accessService.validateAdminAccess(adminId, role);
        EmergencyRiderReplacementReason reasonCode = request.reasonCode();
        String reasonDetail = normalizeDetail(request.reasonDetail());
        if (reasonCode.requiresDetail() && reasonDetail == null) {
            throw new OtherReasonDetailRequiredException();
        }

        DeliveryAssignment reference = assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId)
            .orElseThrow(DeliveryAssignmentNotFoundException::new);
        Long groupId = reference.getDeliveryGroup().getId();
        Long previousRiderId = reference.getRider().getId();

        DeliveryGroup group = groupRepository.findByIdForUpdate(groupId)
            .orElseThrow(DeliveryAssignmentNotFoundException::new);
        if (group.getStatus() != DeliveryGroupStatus.CONFIRMED
            || group.getActualStartedAt() != null) {
            throw new DeliveryAssignmentStateConflictException();
        }
        if (previousRiderId.equals(request.newRiderId())) {
            throw new AssignmentConditionNotMetException();
        }

        List<Long> riderIds = new ArrayList<>(List.of(previousRiderId, request.newRiderId()));
        riderIds.sort(Comparator.naturalOrder());
        List<Rider> riders = riderRepository.findAllByIdInForUpdate(riderIds);
        Rider newRider = riders.stream()
            .filter(rider -> rider.getId().equals(request.newRiderId()))
            .findFirst().orElseThrow(RiderNotFoundException::new);

        List<Delivery> deliveries = deliveryRepository.findAllByDeliveryGroupIdForUpdate(groupId);
        List<DeliveryAssignment> assignments =
            assignmentRepository.findAllByDeliveryGroupIdForUpdate(groupId);
        List<DeliveryAssignmentItem> items =
            itemRepository.findAllByDeliveryGroupIdForUpdate(groupId);
        DeliveryAssignment original = assignments.stream()
            .filter(assignment -> assignment.getId().equals(assignmentId))
            .findFirst().orElseThrow(DeliveryAssignmentNotFoundException::new);
        if (original.getStatus() != DeliveryAssignmentStatus.CONFIRMED) {
            throw new DeliveryAssignmentStateConflictException();
        }

        List<DeliveryAssignmentItem> originalItems = items.stream()
            .filter(item -> item.getAssignment().getId().equals(assignmentId))
            .toList();
        if (originalItems.isEmpty()
            || originalItems.stream().anyMatch(item -> item.getDelivery().getStatus() != DeliveryStatus.READY)) {
            throw new DeliveryAssignmentStateConflictException();
        }
        validateExclusiveOwnership(originalItems, assignments, items, assignmentId);
        validateEligibilityAndCapacity(newRider, group, originalItems, assignments, items);

        LocalDateTime replacedAt = LocalDateTime.now(KST);
        if (assignmentRepository.reassignIfConfirmed(
            assignmentId, DeliveryAssignmentStatus.CONFIRMED, DeliveryAssignmentStatus.REASSIGNED
        ) != 1) {
            throw new DeliveryAssignmentStateConflictException();
        }

        DeliveryAssignment replacement = assignmentRepository.save(
            new DeliveryAssignment(
                group, newRider, DeliveryAssignmentType.MANUAL, replacedAt, adminId
            )
        );
        itemRepository.saveAll(
            originalItems.stream()
                .map(item -> new DeliveryAssignmentItem(replacement, item.getDelivery()))
                .toList()
        );
        auditRepository.save(
            AuditHistory.record(
                "DELIVERY_ASSIGNMENT", assignmentId, "EMERGENCY_RIDER_REPLACED"
                , adminId, AuditActorType.ADMIN, reasonCode.name(), reasonDetail
                , "{\"assignmentId\":" + assignmentId + ",\"riderId\":" + previousRiderId + "}"
                , "{\"assignmentId\":" + replacement.getId() + ",\"riderId\":"
                    + newRider.getId() + "}"
                , replacedAt
            )
        );
        entityManager.refresh(original);
        eventPublisher.publishEvent(new RiderAssignmentReassignedEvent(replacement.getId()));

        return new AdminEmergencyRiderReplacementResponse(
            original.getId(), original.getStatus(), replacement.getId(), replacement.getStatus()
            , previousRiderId, newRider.getId(), replacedAt.atZone(KST).toOffsetDateTime()
        );
    }

    private void validateExclusiveOwnership(
        List<DeliveryAssignmentItem> originalItems
        , List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> items
        , Long assignmentId
    ) {
        List<Long> activeIds = assignments.stream()
            .filter(assignment -> assignment.getStatus().isActive())
            .map(DeliveryAssignment::getId).toList();
        for (DeliveryAssignmentItem originalItem : originalItems) {
            List<DeliveryAssignmentItem> owners = items.stream()
                .filter(item -> activeIds.contains(item.getAssignment().getId()))
                .filter(item -> item.getDelivery().getId().equals(originalItem.getDelivery().getId()))
                .toList();
            if (owners.size() != 1
                || !owners.getFirst().getAssignment().getId().equals(assignmentId)) {
                throw new DeliveryAssignmentStateConflictException();
            }
        }
    }

    private void validateEligibilityAndCapacity(
        Rider newRider
        , DeliveryGroup group
        , List<DeliveryAssignmentItem> originalItems
        , List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> items
    ) {
        for (DeliveryAssignmentItem item : originalItems) {
            if (!eligibilityService.isEligible(
                newRider, group.getDeliveryDate(), group.getSlot().getCode()
                , item.getDelivery().getDeliveryAreaCode()
            )) {
                throw new AssignmentConditionNotMetException();
            }
        }
        List<Long> activeIds = assignments.stream()
            .filter(assignment -> assignment.getStatus().isActive())
            .filter(assignment -> assignment.getRider().getId().equals(newRider.getId()))
            .map(DeliveryAssignment::getId).toList();
        RiderAssignmentLoad load = new RiderAssignmentLoad(0, 0);
        items.stream()
            .filter(item -> activeIds.contains(item.getAssignment().getId()))
            .forEach(item -> load.add(item.getDelivery().getLunchboxQuantity()));
        for (DeliveryAssignmentItem item : originalItems) {
            if (!load.canAssign(item.getDelivery().getLunchboxQuantity())) {
                throw new DeliveryCapacityExceededException();
            }
            load.add(item.getDelivery().getLunchboxQuantity());
        }
    }

    private String normalizeDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        return detail.trim();
    }
}
