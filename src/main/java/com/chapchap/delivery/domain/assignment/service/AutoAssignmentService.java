package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.event.RiderAssignmentAvailableEvent;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class AutoAssignmentService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryGroupRepository deliveryGroupRepository;
    private final RiderRepository riderRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    private final DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository;
    private final RiderAssignmentEligibilityService riderAssignmentEligibilityService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AutoAssignmentService(
        DeliveryGroupRepository deliveryGroupRepository
        , RiderRepository riderRepository
        , DeliveryRepository deliveryRepository
        , DeliveryAssignmentRepository deliveryAssignmentRepository
        , DeliveryAssignmentItemRepository deliveryAssignmentItemRepository
        , DeliveryGroupStatusHistoryRepository deliveryGroupStatusHistoryRepository
        , RiderAssignmentEligibilityService riderAssignmentEligibilityService
        , ApplicationEventPublisher applicationEventPublisher
    ) {
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.riderRepository = riderRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryAssignmentItemRepository = deliveryAssignmentItemRepository;
        this.deliveryGroupStatusHistoryRepository = deliveryGroupStatusHistoryRepository;
        this.riderAssignmentEligibilityService = riderAssignmentEligibilityService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public boolean assign(Long deliveryGroupId) {
        DeliveryGroup deliveryGroup =
            deliveryGroupRepository.findByIdForUpdate(
                    deliveryGroupId
                )
                .orElse(null);

        if (
            deliveryGroup == null
                || !deliveryGroup.isWaitingAutoAssignment()
        ) {
            return false;
        }

        List<Rider> riderCandidates =
            riderRepository.findAllByDeletedAtIsNullOrderByIdAsc();

        if (riderCandidates.isEmpty()) {
            return false;
        }

        List<Long> riderIds =
            riderCandidates.stream()
                .map(Rider::getId)
                .toList();

        List<Rider> riders =
            riderRepository.findAllByIdInForUpdate(
                riderIds
            );

        List<Delivery> deliveries =
            deliveryRepository.findAllByDeliveryGroupIdForUpdate(
                deliveryGroupId
            );

        if (
            deliveries.isEmpty()
                || hasNonReadyDelivery(deliveries)
        ) {
            return false;
        }

        List<DeliveryAssignment> assignments =
            deliveryAssignmentRepository.findAllByDeliveryGroupIdForUpdate(
                deliveryGroupId
            );

        List<DeliveryAssignmentItem> assignmentItems =
            deliveryAssignmentItemRepository.findAllByDeliveryGroupIdForUpdate(
                deliveryGroupId
            );

        if (hasActiveAssignment(
            assignments
            , assignmentItems
        )) {
            return false;
        }

        Map<Long, RiderAssignmentLoad> riderLoadMap =
            createRiderLoadMap(
                assignments
                , assignmentItems
            );

        Map<Long, Rider> riderMap =
            createRiderMap(
                riders
            );

        Map<Long, List<Delivery>> assignmentPlan =
            createAssignmentPlan(
                riders
                , deliveryGroup
                , deliveries
                , riderLoadMap
            );

        if (assignmentPlan == null) {
            return false;
        }

        LocalDateTime assignedAt =
            LocalDateTime.now(KST);

        List<DeliveryAssignment> savedAssignments =
            saveAssignments(
                deliveryGroup
                , assignmentPlan
                , riderMap
                , assignedAt
            );

        deliveryGroup.completeAutoAssignment(
            assignedAt
        );

        deliveryGroupStatusHistoryRepository.save(
            new DeliveryGroupStatusHistory(
                deliveryGroup
                , DeliveryGroupStatus.WAITING_ASSIGNMENT
                , DeliveryGroupStatus.WAITING_RIDER
                , null
                , DeliveryGroupChangedByType.SYSTEM
                , assignedAt
            )
        );

        for (DeliveryAssignment assignment : savedAssignments) {
            applicationEventPublisher.publishEvent(
                new RiderAssignmentAvailableEvent(
                    assignment.getId()
                )
            );
        }

        return true;
    }

    private boolean hasNonReadyDelivery(List<Delivery> deliveries) {
        for (Delivery delivery : deliveries) {
            if (delivery.getStatus() != DeliveryStatus.READY) {
                return true;
            }
        }

        return false;
    }

    private boolean hasActiveAssignment(
        List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> assignmentItems
    ) {
        Set<Long> activeAssignmentIds =
            new HashSet<>();

        for (DeliveryAssignment assignment : assignments) {
            if (assignment.getStatus().isActive()) {
                activeAssignmentIds.add(
                    assignment.getId()
                );
            }
        }

        for (DeliveryAssignmentItem assignmentItem : assignmentItems) {
            if (
                activeAssignmentIds.contains(
                    assignmentItem.getAssignment().getId()
                )
            ) {
                return true;
            }
        }

        return false;
    }

    private Map<Long, Rider> createRiderMap(List<Rider> riders) {
        Map<Long, Rider> riderMap =
            new HashMap<>();

        for (Rider rider : riders) {
            riderMap.put(
                rider.getId()
                , rider
            );
        }

        return riderMap;
    }

    private Map<Long, List<Delivery>> createAssignmentPlan(
        List<Rider> riders
        , DeliveryGroup deliveryGroup
        , List<Delivery> deliveries
        , Map<Long, RiderAssignmentLoad> riderLoadMap
    ) {
        Map<Long, List<Delivery>> assignmentPlan =
            new LinkedHashMap<>();

        for (Delivery delivery : deliveries) {
            List<Rider> assignableRiders =
                findAssignableRiders(
                    riders
                    , deliveryGroup
                    , delivery
                    , riderLoadMap
                );

            if (assignableRiders.isEmpty()) {
                return null;
            }

            Rider selectedRider =
                findPreferredRider(
                    assignableRiders
                    , riderLoadMap
                    , delivery.getLunchboxQuantity()
                );

            assignmentPlan
                .computeIfAbsent(
                    selectedRider.getId()
                    , ignored -> new ArrayList<>()
                )
                .add(
                    delivery
                );

            riderLoadMap
                .get(selectedRider.getId())
                .add(
                    delivery.getLunchboxQuantity()
                );
        }

        return assignmentPlan;
    }

    private List<DeliveryAssignment> saveAssignments(
        DeliveryGroup deliveryGroup
        , Map<Long, List<Delivery>> assignmentPlan
        , Map<Long, Rider> riderMap
        , LocalDateTime assignedAt
    ) {
        List<DeliveryAssignment> savedAssignments =
            new ArrayList<>();

        for (
            Map.Entry<Long, List<Delivery>> entry
            : assignmentPlan.entrySet()
        ) {
            Rider rider =
                riderMap.get(
                    entry.getKey()
                );

            DeliveryAssignment assignment =
                deliveryAssignmentRepository.save(
                    new DeliveryAssignment(
                        deliveryGroup
                        , rider
                        , assignedAt
                    )
                );

            List<DeliveryAssignmentItem> assignmentItems =
                new ArrayList<>();

            for (Delivery delivery : entry.getValue()) {
                assignmentItems.add(
                    new DeliveryAssignmentItem(
                        assignment
                        , delivery
                    )
                );
            }

            deliveryAssignmentItemRepository.saveAll(
                assignmentItems
            );

            savedAssignments.add(
                assignment
            );
        }

        return savedAssignments;
    }

    private Map<Long, RiderAssignmentLoad> createRiderLoadMap(
        List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> assignmentItems
    ) {
        Map<Long, DeliveryAssignment> activeAssignmentMap =
            new HashMap<>();

        for (DeliveryAssignment assignment : assignments) {
            if (assignment.getStatus().isActive()) {
                activeAssignmentMap.put(
                    assignment.getId()
                    , assignment
                );
            }
        }

        Map<Long, RiderAssignmentLoad> riderLoadMap =
            new HashMap<>();

        for (DeliveryAssignmentItem assignmentItem : assignmentItems) {
            DeliveryAssignment assignment =
                activeAssignmentMap.get(
                    assignmentItem.getAssignment().getId()
                );

            if (assignment == null) {
                continue;
            }

            Long riderId =
                assignment.getRider().getId();

            RiderAssignmentLoad riderLoad =
                riderLoadMap.computeIfAbsent(
                    riderId
                    , ignored -> new RiderAssignmentLoad(0, 0)
                );

            riderLoad.add(
                assignmentItem.getDelivery().getLunchboxQuantity()
            );
        }

        return riderLoadMap;
    }

    private List<Rider> findAssignableRiders(
        List<Rider> riders
        , DeliveryGroup deliveryGroup
        , Delivery delivery
        , Map<Long, RiderAssignmentLoad> riderLoadMap
    ) {
        List<Rider> assignableRiders =
            new ArrayList<>();

        for (Rider rider : riders) {
            boolean eligible =
                riderAssignmentEligibilityService.isEligible(
                    rider
                    , deliveryGroup.getDeliveryDate()
                    , deliveryGroup.getSlot().getCode()
                    , delivery.getDeliveryAreaCode()
                );

            if (!eligible) {
                continue;
            }

            RiderAssignmentLoad riderLoad =
                riderLoadMap.computeIfAbsent(
                    rider.getId()
                    , ignored -> new RiderAssignmentLoad(0, 0)
                );

            if (!riderLoad.canAssign(
                delivery.getLunchboxQuantity()
            )) {
                continue;
            }

            assignableRiders.add(
                rider
            );
        }

        return assignableRiders;
    }

    private Rider findPreferredRider(
        List<Rider> assignableRiders
        , Map<Long, RiderAssignmentLoad> riderLoadMap
        , Integer additionalLunchboxQuantity
    ) {
        List<Rider> recommendedRiders =
            assignableRiders.stream()
                .filter(
                    rider -> riderLoadMap
                        .computeIfAbsent(
                            rider.getId()
                            , ignored -> new RiderAssignmentLoad(0, 0)
                        )
                        .canAssignWithinRecommended(additionalLunchboxQuantity)
                )
                .toList();

        List<Rider> candidates = new ArrayList<>(
            recommendedRiders.isEmpty() ? assignableRiders : recommendedRiders
        );

        candidates.sort(
            Comparator
                .comparingInt(
                    (Rider rider) -> riderLoadMap
                        .computeIfAbsent(
                            rider.getId()
                            , ignored -> new RiderAssignmentLoad(0, 0)
                        )
                        .getVisitCount()
                )
                .thenComparingInt(
                    rider -> riderLoadMap
                        .get(rider.getId())
                        .getLunchboxQuantity()
                )
                .thenComparing(Rider::getId)
        );

        return candidates.getFirst();
    }
}