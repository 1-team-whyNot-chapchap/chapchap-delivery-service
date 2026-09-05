package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.response.DeliveryGroupConfirmationResponse;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.entity.DeliveryRecipientSnapshot;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.exception.business.DeliveryGroupConfirmationConditionNotMetException;
import com.chapchap.delivery.global.exception.business.DeliveryGroupNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminDeliveryGroupConfirmationService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String ENTITY_TYPE = "DELIVERY_GROUP";
    private static final String ACTION = "DELIVERY_GROUP_CONFIRMED";

    private final DeliveryGroupRepository deliveryGroupRepository;
    private final RiderRepository riderRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryRecipientSnapshotRepository deliveryRecipientSnapshotRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    private final DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository;
    private final DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    private final AuditHistoryRepository auditHistoryRepository;
    private final DeliveryAccessService deliveryAccessService;
    private final RiderAssignmentEligibilityService riderAssignmentEligibilityService;

    public AdminDeliveryGroupConfirmationService(
        DeliveryGroupRepository deliveryGroupRepository
        , RiderRepository riderRepository
        , DeliveryRepository deliveryRepository
        , DeliveryRecipientSnapshotRepository deliveryRecipientSnapshotRepository
        , DeliveryAssignmentRepository deliveryAssignmentRepository
        , DeliveryAssignmentItemRepository deliveryAssignmentItemRepository
        , DeliveryAssignmentIssueRepository deliveryAssignmentIssueRepository
        , DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository
        , AuditHistoryRepository auditHistoryRepository
        , DeliveryAccessService deliveryAccessService
        , RiderAssignmentEligibilityService riderAssignmentEligibilityService
    ) {
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.riderRepository = riderRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryRecipientSnapshotRepository = deliveryRecipientSnapshotRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryAssignmentItemRepository = deliveryAssignmentItemRepository;
        this.deliveryAssignmentIssueRepository = deliveryAssignmentIssueRepository;
        this.deliveryGroupStatusHistoryRepository = deliveryGroupStatusHistoryRepository;
        this.auditHistoryRepository = auditHistoryRepository;
        this.deliveryAccessService = deliveryAccessService;
        this.riderAssignmentEligibilityService = riderAssignmentEligibilityService;
    }

    @Transactional
    public DeliveryGroupConfirmationResponse confirm(
        Long actorId
        , UserRole actorRole
        , Long deliveryGroupId
    ) {
        deliveryAccessService.validateAdminAccess(actorId, actorRole);

        DeliveryGroup deliveryGroup =
            deliveryGroupRepository.findByIdForUpdate(deliveryGroupId)
                .orElseThrow(DeliveryGroupNotFoundException::new);

        if (deliveryGroup.getStatus() != DeliveryGroupStatus.READY_TO_CONFIRM) {
            throw new DeliveryGroupConfirmationConditionNotMetException();
        }

        List<Long> riderIds =
            deliveryAssignmentRepository.findActiveRiderIdsByDeliveryGroupId(
                deliveryGroupId
                , DeliveryAssignmentStatus.REASSIGNED
            );

        List<Rider> riders = riderRepository.findAllByIdInForUpdate(riderIds);
        List<Delivery> deliveries =
            deliveryRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId);
        List<DeliveryRecipientSnapshot> recipientSnapshots =
            deliveryRecipientSnapshotRepository.findAllByDeliveryIdIn(
                deliveries.stream().map(Delivery::getId).toList()
            );
        List<DeliveryAssignment> assignments =
            deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId);
        List<DeliveryAssignmentItem> assignmentItems =
            deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(deliveryGroupId);

        validateConfirmationConditions(
            deliveryGroup
            , riders
            , deliveries
            , recipientSnapshots
            , assignments
            , assignmentItems
        );

        LocalDateTime confirmedAt = LocalDateTime.now(KST);

        assignments.stream()
            .filter(assignment -> assignment.getStatus().isActive())
            .forEach(assignment -> assignment.confirm(actorId, confirmedAt));

        deliveryGroup.confirm();

        deliveryGroupStatusHistoryRepository.save(
            new DeliveryGroupStatusHistory(
                deliveryGroup
                , DeliveryGroupStatus.READY_TO_CONFIRM
                , DeliveryGroupStatus.CONFIRMED
                , actorId
                , DeliveryGroupChangedByType.ADMIN
                , confirmedAt
            )
        );

        auditHistoryRepository.save(
            AuditHistory.record(
                ENTITY_TYPE
                , deliveryGroupId
                , ACTION
                , actorId
                , AuditActorType.ADMIN
                , null
                , null
                , "{\"status\":\"READY_TO_CONFIRM\"}"
                , "{\"status\":\"CONFIRMED\"}"
                , confirmedAt
            )
        );

        return new DeliveryGroupConfirmationResponse(
            deliveryGroupId
            , DeliveryGroupStatus.CONFIRMED
            , confirmedAt.atZone(KST).toOffsetDateTime()
            , actorId
        );
    }

    private void validateConfirmationConditions(
        DeliveryGroup deliveryGroup
        , List<Rider> riders
        , List<Delivery> deliveries
        , List<DeliveryRecipientSnapshot> recipientSnapshots
        , List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> assignmentItems
    ) {
        if (deliveries.isEmpty() || assignments.isEmpty()) {
            throw new DeliveryGroupConfirmationConditionNotMetException();
        }

        if (deliveryAssignmentIssueRepository.countUnresolvedByDeliveryGroupId(
            deliveryGroup.getId()
        ) > 0) {
            throw new DeliveryGroupConfirmationConditionNotMetException();
        }

        Map<Long, Rider> riderMap = new HashMap<>();
        for (Rider rider : riders) {
            riderMap.put(rider.getId(), rider);
        }

        Set<Long> activeAssignmentIds = new HashSet<>();
        Map<Long, RiderAssignmentLoad> loadByRiderId = new HashMap<>();

        for (DeliveryAssignment assignment : assignments) {
            if (!assignment.getStatus().isActive()) {
                continue;
            }
            if (!assignment.isAcknowledged() || !riderMap.containsKey(assignment.getRider().getId())) {
                throw new DeliveryGroupConfirmationConditionNotMetException();
            }
            activeAssignmentIds.add(assignment.getId());
            loadByRiderId.computeIfAbsent(
                assignment.getRider().getId()
                , ignored -> new RiderAssignmentLoad(0, 0)
            );
        }

        Map<Long, Integer> ownerCounts = new HashMap<>();
        for (DeliveryAssignmentItem item : assignmentItems) {
            Long assignmentId = item.getAssignment().getId();
            if (!activeAssignmentIds.contains(assignmentId)) {
                continue;
            }

            Delivery delivery = item.getDelivery();
            if (delivery.getStatus() != DeliveryStatus.READY) {
                throw new DeliveryGroupConfirmationConditionNotMetException();
            }

            Rider rider = riderMap.get(item.getAssignment().getRider().getId());
            if (!riderAssignmentEligibilityService.isEligible(
                rider
                , deliveryGroup.getDeliveryDate()
                , deliveryGroup.getSlot().getCode()
                , delivery.getDeliveryAreaCode()
            )) {
                throw new DeliveryGroupConfirmationConditionNotMetException();
            }

            RiderAssignmentLoad load = loadByRiderId.get(rider.getId());
            if (!load.canAssign(delivery.getLunchboxQuantity())) {
                throw new DeliveryGroupConfirmationConditionNotMetException();
            }
            load.add(delivery.getLunchboxQuantity());
            ownerCounts.merge(delivery.getId(), 1, Integer::sum);
        }

        if (deliveries.stream().anyMatch(
            delivery ->
                delivery.getStatus() != DeliveryStatus.READY
                    || ownerCounts.getOrDefault(delivery.getId(), 0) != 1
        )) {
            throw new DeliveryGroupConfirmationConditionNotMetException();
        }

        Set<Long> snapshotDeliveryIds = recipientSnapshots.stream()
            .map(DeliveryRecipientSnapshot::getDeliveryId)
            .collect(Collectors.toSet());

        if (deliveries.stream().anyMatch(
            delivery -> !snapshotDeliveryIds.contains(delivery.getId())
        )) {
            throw new DeliveryGroupConfirmationConditionNotMetException();
        }
    }
}
