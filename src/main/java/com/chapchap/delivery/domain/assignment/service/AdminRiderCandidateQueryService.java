package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentCapacity;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.response.AdminRiderCandidateListResponse;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.service.RiderDeliveryAreaService;
import com.chapchap.delivery.global.exception.business.DeliveryAssignmentNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryGroupNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRiderCandidateQueryService {
    private final DeliveryAccessService deliveryAccessService;
    private final DeliveryGroupRepository deliveryGroupRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryAssignmentItemRepository deliveryAssignmentItemRepository;
    private final RiderRepository riderRepository;
    private final RiderAssignmentEligibilityService riderAssignmentEligibilityService;
    private final RiderDeliveryAreaService riderDeliveryAreaService;

    public AdminRiderCandidateQueryService(
        DeliveryAccessService deliveryAccessService
        , DeliveryGroupRepository deliveryGroupRepository
        , DeliveryRepository deliveryRepository
        , DeliveryAssignmentRepository deliveryAssignmentRepository
        , DeliveryAssignmentItemRepository deliveryAssignmentItemRepository
        , RiderRepository riderRepository
        , RiderAssignmentEligibilityService riderAssignmentEligibilityService
        , RiderDeliveryAreaService riderDeliveryAreaService
    ) {
        this.deliveryAccessService = deliveryAccessService;
        this.deliveryGroupRepository = deliveryGroupRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryAssignmentItemRepository = deliveryAssignmentItemRepository;
        this.riderRepository = riderRepository;
        this.riderAssignmentEligibilityService = riderAssignmentEligibilityService;
        this.riderDeliveryAreaService = riderDeliveryAreaService;
    }

    @Transactional(readOnly = true)
    public AdminRiderCandidateListResponse getCandidates(
        Long actorId
        , UserRole actorRole
        , Long deliveryGroupId
        , Long assignmentId
    ) {
        deliveryAccessService.validateAdminAccess(actorId, actorRole);

        DeliveryGroup group = deliveryGroupRepository.findDetailById(deliveryGroupId)
            .orElseThrow(DeliveryGroupNotFoundException::new);
        List<Delivery> deliveries = deliveryRepository.findAllByDeliveryGroupIdIn(List.of(deliveryGroupId));
        List<DeliveryAssignment> assignments =
            deliveryAssignmentRepository.findAllByDeliveryGroupIdIn(List.of(deliveryGroupId));
        List<DeliveryAssignmentItem> items =
            deliveryAssignmentItemRepository.findAllByDeliveryGroupIdIn(List.of(deliveryGroupId));

        DeliveryAssignment targetAssignment = findTargetAssignment(assignments, assignmentId);
        List<Delivery> targetDeliveries = targetDeliveries(deliveries, items, targetAssignment);
        Map<Long, Load> assignedLoadByRider = assignedLoadByRider(items, targetAssignment);
        boolean operationAvailable = isOperationAvailable(group, targetAssignment, targetDeliveries);

        List<AdminRiderCandidateListResponse.Item> candidates = riderRepository
            .findAllByDeletedAtIsNullOrderByIdAsc()
            .stream()
            .filter(rider -> targetAssignment == null
                || !rider.getId().equals(targetAssignment.getRider().getId()))
            .map(rider -> toItem(rider, group, targetDeliveries, assignedLoadByRider.get(rider.getId()),
                targetAssignment != null, operationAvailable))
            .toList();

        return new AdminRiderCandidateListResponse(
            group.getId(), group.getDeliveryDate(), group.getSlot().getCode(), candidates
        );
    }

    private DeliveryAssignment findTargetAssignment(
        List<DeliveryAssignment> assignments
        , Long assignmentId
    ) {
        if (assignmentId == null) {
            return null;
        }
        return assignments.stream()
            .filter(assignment -> assignment.getId().equals(assignmentId))
            .findFirst()
            .orElseThrow(DeliveryAssignmentNotFoundException::new);
    }

    private List<Delivery> targetDeliveries(
        List<Delivery> deliveries
        , List<DeliveryAssignmentItem> items
        , DeliveryAssignment targetAssignment
    ) {
        if (targetAssignment != null) {
            return items.stream()
                .filter(item -> item.getAssignment().getId().equals(targetAssignment.getId()))
                .map(DeliveryAssignmentItem::getDelivery)
                .toList();
        }

        Set<Long> assignedDeliveryIds = items.stream()
            .filter(item -> item.getAssignment().getStatus().isActive())
            .map(item -> item.getDelivery().getId())
            .collect(Collectors.toSet());
        return deliveries.stream()
            .filter(delivery -> !assignedDeliveryIds.contains(delivery.getId()))
            .toList();
    }

    private Map<Long, Load> assignedLoadByRider(
        List<DeliveryAssignmentItem> items
        , DeliveryAssignment targetAssignment
    ) {
        Long targetAssignmentId = targetAssignment == null ? null : targetAssignment.getId();
        Map<Long, Load> result = new HashMap<>();
        items.stream()
            .filter(item -> item.getAssignment().getStatus().isActive())
            .filter(item -> !item.getAssignment().getId().equals(targetAssignmentId))
            .forEach(item -> result.computeIfAbsent(item.getAssignment().getRider().getId(), ignored -> new Load())
                .add(item.getDelivery().getLunchboxQuantity()));
        return result;
    }

    private boolean isOperationAvailable(
        DeliveryGroup group
        , DeliveryAssignment targetAssignment
        , List<Delivery> targetDeliveries
    ) {
        if (targetDeliveries.isEmpty()
            || targetDeliveries.stream().anyMatch(delivery -> delivery.getStatus() != DeliveryStatus.READY)) {
            return false;
        }
        if (targetAssignment == null) {
            return group.isWaitingAutoAssignment();
        }
        if (targetAssignment.getStatus() == DeliveryAssignmentStatus.ISSUE_REPORTED) {
            return group.isIssueReview();
        }
        return targetAssignment.getStatus() == DeliveryAssignmentStatus.CONFIRMED
            && group.getStatus() == DeliveryGroupStatus.CONFIRMED
            && group.getActualStartedAt() == null;
    }

    private AdminRiderCandidateListResponse.Item toItem(
        Rider rider
        , DeliveryGroup group
        , List<Delivery> targetDeliveries
        , Load assignedLoad
        , boolean isReplacement
        , boolean operationAvailable
    ) {
        Load currentLoad = assignedLoad == null ? new Load() : assignedLoad;
        Load projectedLoad = currentLoad.copy();
        targetDeliveries.forEach(delivery -> projectedLoad.add(delivery.getLunchboxQuantity()));
        Load capacityLoad = isReplacement ? projectedLoad : currentLoad;

        boolean isAreaMatched = targetDeliveries.stream().allMatch(delivery ->
            riderDeliveryAreaService.canDeliverToArea(
                rider.getId(), delivery.getDeliveryAreaCode(), group.getDeliveryDate()
            )
        );
        boolean canWork = riderAssignmentEligibilityService.isEligibleIgnoringArea(
            rider, group.getDeliveryDate(), group.getSlot().getCode()
        );
        boolean maximumCapacityExceeded = isMaximumCapacityExceeded(capacityLoad);
        boolean eligibleForAreaPolicy = !isReplacement || isAreaMatched;
        boolean isEligible = !targetDeliveries.isEmpty()
            && operationAvailable
            && canWork
            && eligibleForAreaPolicy
            && !maximumCapacityExceeded;

        return new AdminRiderCandidateListResponse.Item(
            rider.getId()
            , rider.getIsDeliveryActive()
            , isEligible
            , currentLoad.stopCount
            , currentLoad.lunchboxQuantity
            , isRecommendedCapacityExceeded(capacityLoad)
            , maximumCapacityExceeded
            , isAreaMatched
        );
    }

    private boolean isRecommendedCapacityExceeded(Load load) {
        return load.stopCount > DeliveryAssignmentCapacity.RECOMMENDED_VISIT_COUNT
            || load.lunchboxQuantity > DeliveryAssignmentCapacity.RECOMMENDED_LUNCHBOX_QUANTITY;
    }

    private boolean isMaximumCapacityExceeded(Load load) {
        return load.stopCount > DeliveryAssignmentCapacity.MAX_VISIT_COUNT
            || load.lunchboxQuantity > DeliveryAssignmentCapacity.MAX_LUNCHBOX_QUANTITY;
    }

    private static final class Load {
        private int stopCount;
        private int lunchboxQuantity;

        private void add(Integer quantity) {
            stopCount++;
            lunchboxQuantity += quantity;
        }

        private Load copy() {
            Load copy = new Load();
            copy.stopCount = stopCount;
            copy.lunchboxQuantity = lunchboxQuantity;
            return copy;
        }
    }
}
