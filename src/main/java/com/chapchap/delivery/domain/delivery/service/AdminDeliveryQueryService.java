package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentCapacity;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentEligibilityService;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryResultType;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryCompletion;
import com.chapchap.delivery.domain.delivery.entity.DeliveryDelay;
import com.chapchap.delivery.domain.delivery.entity.DeliveryFailure;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.entity.DeliveryResultCorrection;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionPhotoRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryCompletionRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryFailureRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryResultCorrectionRepository;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryDetailResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryGroupDetailResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryGroupListItemResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryGroupListResponse;
import com.chapchap.delivery.global.exception.business.DeliveryGroupNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDeliveryQueryService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryAccessService accessService;
    private final DeliveryGroupRepository groupRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentItemRepository assignmentItemRepository;
    private final DeliveryAssignmentIssueRepository issueRepository;
    private final DeliveryRecipientSnapshotRepository recipientSnapshotRepository;
    private final DeliveryDelayRepository delayRepository;
    private final DeliveryCompletionRepository completionRepository;
    private final DeliveryCompletionPhotoRepository photoRepository;
    private final DeliveryFailureRepository failureRepository;
    private final DeliveryStatusHistoryRepository statusHistoryRepository;
    private final DeliveryResultCorrectionRepository correctionRepository;
    private final RiderAssignmentEligibilityService eligibilityService;

    @Transactional(readOnly = true)
    public AdminDeliveryGroupListResponse getDeliveryGroups(
        Long actorId
        , UserRole role
        , LocalDate deliveryDate
        , DeliverySlotCode slotCode
        , DeliveryGroupStatus status
        , Pageable pageable
    ) {
        accessService.validateAdminAccess(actorId, role);
        Page<DeliveryGroup> groups = groupRepository.findAllForAdmin(
            deliveryDate
            , slotCode
            , status
            , DeliveryQueryPageable.adminGroup(pageable)
        );
        GroupData data = loadGroupData(groups.getContent());
        return AdminDeliveryGroupListResponse.from(
            groups.map(group -> toListItem(group, data))
        );
    }

    @Transactional(readOnly = true)
    public AdminDeliveryGroupDetailResponse getDeliveryGroup(
        Long actorId
        , UserRole role
        , Long deliveryGroupId
    ) {
        accessService.validateAdminAccess(actorId, role);
        DeliveryGroup group = groupRepository.findDetailById(deliveryGroupId)
            .orElseThrow(DeliveryGroupNotFoundException::new);
        GroupData data = loadGroupData(List.of(group));

        List<Delivery> deliveries = data.deliveriesByGroup().getOrDefault(group.getId(), List.of());
        List<DeliveryAssignment> assignments = data.assignmentsByGroup().getOrDefault(group.getId(), List.of());
        List<DeliveryAssignmentItem> items = data.itemsByGroup().getOrDefault(group.getId(), List.of());
        Set<Long> delayedIds = data.delayedDeliveryIds();
        Map<Long, DeliveryAssignment> activeOwnerByDelivery = activeOwnerByDelivery(items);

        List<AdminDeliveryGroupDetailResponse.Assignment> assignmentResponses = assignments.stream()
            .map(assignment -> toAssignment(assignment, items))
            .toList();
        List<AdminDeliveryGroupDetailResponse.Issue> issueResponses = data.issuesByGroup()
            .getOrDefault(group.getId(), List.of()).stream()
            .map(this::toIssue)
            .toList();
        List<AdminDeliveryGroupDetailResponse.DeliveryItem> deliveryResponses = deliveries.stream()
            .map(delivery -> {
                DeliveryAssignment owner = activeOwnerByDelivery.get(delivery.getId());
                return new AdminDeliveryGroupDetailResponse.DeliveryItem(
                    delivery.getDeliveryPublicId()
                    , delivery.getCustomerId()
                    , delivery.getStatus()
                    , delivery.getLunchboxQuantity()
                    , owner == null ? null : owner.getId()
                    , delayedIds.contains(delivery.getId())
                );
            })
            .toList();

        return new AdminDeliveryGroupDetailResponse(
            group.getId()
            , group.getDeliveryDate()
            , group.getSlot().getCode()
            , group.getStatus()
            , toOffset(group.getAutoAssignmentCompletedAt())
            , toOffset(group.getActualStartedAt())
            , toOffset(group.getActualFinishedAt())
            , confirmationTime(group, 9, 30, 15, 30)
            , confirmationTime(group, 10, 0, 16, 0)
            , readiness(group, deliveries, assignments, items, data)
            , assignmentResponses
            , issueResponses
            , deliveryResponses
        );
    }

    @Transactional(readOnly = true)
    public AdminDeliveryDetailResponse getDelivery(
        Long actorId
        , UserRole role
        , String deliveryPublicId
    ) {
        accessService.validateAdminAccess(actorId, role);
        Delivery delivery = deliveryRepository.findDetailByDeliveryPublicId(deliveryPublicId)
            .orElseThrow(DeliveryNotFoundException::new);
        DeliveryDelay delay = delayRepository.findByDeliveryId(delivery.getId()).orElse(null);
        DeliveryCompletion completion = completionRepository.findByDeliveryId(delivery.getId()).orElse(null);
        DeliveryFailure failure = failureRepository.findByDeliveryId(delivery.getId()).orElse(null);
        boolean hasPhoto = completion != null
            && photoRepository.findByDeliveryCompletionId(completion.getId()).isPresent();

        List<AdminDeliveryDetailResponse.StatusHistory> statusHistories =
            statusHistoryRepository.findAllByDelivery_IdOrderByChangedAtAsc(delivery.getId())
                .stream().map(this::toStatusHistory).toList();
        List<AdminDeliveryDetailResponse.AssignmentHistory> assignmentHistories =
            assignmentRepository.findAllByDeliveryId(delivery.getId()).stream()
                .map(this::toAssignmentHistory).toList();
        List<DeliveryResultCorrection> correctionEntities =
            correctionRepository.findAllByDelivery_IdOrderByIdAsc(delivery.getId());
        List<AdminDeliveryDetailResponse.ResultCorrection> corrections = correctionEntities.stream()
            .map(correction -> new AdminDeliveryDetailResponse.ResultCorrection(
                correction.getId(), correction.getResultType(), correction.getFieldName(),
                correction.getBeforeValue(), correction.getAfterValue(),
                correction.getReasonCode(), correction.getReasonDetail(),
                correction.getCorrectedBy(), toOffset(correction.getCorrectedAt())
            )).toList();
        CompletionValues effectiveCompletion = effectiveCompletionValues(
            completion, correctionEntities
        );
        FailureValues effectiveFailure = effectiveFailureValues(
            failure, correctionEntities
        );

        return new AdminDeliveryDetailResponse(
            delivery.getDeliveryPublicId()
            , delivery.getSourceOrderId()
            , delivery.getCustomerId()
            , delivery.getDeliveryGroup().getId()
            , delivery.getDeliveryGroup().getDeliveryDate()
            , delivery.getDeliveryGroup().getSlot().getCode()
            , delivery.getStatus()
            , delivery.getDeliveryVersion()
            , delivery.getLunchboxQuantity()
            , delivery.getRotationMenuId()
            , delivery.getMenuNameSnapshot()
            , delivery.getRequestHandoffType()
            , delay == null ? null : new AdminDeliveryDetailResponse.Delay(
                delay.getDelayMinutes(), toOffset(delay.getDetectedAt())
            )
            , completion == null ? null : new AdminDeliveryDetailResponse.Completion(
                completion.getActualHandoffType()
                , completion.getStorageLocation()
                , effectiveCompletion.handoffType()
                , effectiveCompletion.storageLocation()
                , toOffset(completion.getContactAttemptedAt())
                , completion.getContactResult()
                , completion.getProcessedBy()
                , completion.getProcessedByType()
                , completion.getAdminReasonCode()
                , completion.getAdminReasonDetail()
                , toOffset(completion.getCompletedAt())
                , hasPhoto
            )
            , failure == null ? null : new AdminDeliveryDetailResponse.Failure(
                failure.getFailureStage()
                , failure.getFailureCode()
                , failure.getFailureDetail()
                , effectiveFailure.failureCode()
                , effectiveFailure.failureDetail()
                , toOffset(failure.getContactAttemptedAt())
                , failure.getContactResult()
                , failure.getItemRecovered()
                , toOffset(failure.getRecoveredAt())
                , failure.getProcessedBy()
                , failure.getProcessedByType()
                , failure.getAdminReasonCode()
                , failure.getAdminReasonDetail()
                , toOffset(failure.getFailedAt())
            )
            , statusHistories
            , assignmentHistories
            , corrections
        );
    }


    private CompletionValues effectiveCompletionValues(
        DeliveryCompletion completion
        , List<DeliveryResultCorrection> corrections
    ) {
        if (completion == null) {
            return null;
        }

        ActualHandoffType handoffType = completion.getActualHandoffType();
        String storageLocation = completion.getStorageLocation();
        for (DeliveryResultCorrection correction : corrections) {
            if (correction.getResultType() != DeliveryResultType.COMPLETION) {
                continue;
            }
            if ("actual_handoff_type".equals(correction.getFieldName())) {
                handoffType = ActualHandoffType.valueOf(correction.getAfterValue());
            } else if ("storage_location".equals(correction.getFieldName())) {
                storageLocation = correction.getAfterValue();
            }
        }
        return new CompletionValues(handoffType, storageLocation);
    }

    private FailureValues effectiveFailureValues(
        DeliveryFailure failure
        , List<DeliveryResultCorrection> corrections
    ) {
        if (failure == null) {
            return null;
        }

        DeliveryFailureCode failureCode = failure.getFailureCode();
        String failureDetail = failure.getFailureDetail();
        for (DeliveryResultCorrection correction : corrections) {
            if (correction.getResultType() != DeliveryResultType.FAILURE) {
                continue;
            }
            if ("failure_code".equals(correction.getFieldName())) {
                failureCode = DeliveryFailureCode.valueOf(correction.getAfterValue());
            } else if ("failure_detail".equals(correction.getFieldName())) {
                failureDetail = correction.getAfterValue();
            }
        }
        return new FailureValues(failureCode, failureDetail);
    }

    private GroupData loadGroupData(Collection<DeliveryGroup> groups) {
        List<Long> groupIds = groups.stream().map(DeliveryGroup::getId).toList();
        if (groupIds.isEmpty()) {
            return GroupData.empty();
        }
        List<Delivery> deliveries = deliveryRepository.findAllByDeliveryGroupIdIn(groupIds);
        List<DeliveryAssignment> assignments = assignmentRepository.findAllByDeliveryGroupIdIn(groupIds);
        List<DeliveryAssignmentItem> items = assignmentItemRepository.findAllByDeliveryGroupIdIn(groupIds);
        List<DeliveryAssignmentIssue> issues = issueRepository.findAllByDeliveryGroupIdIn(groupIds);
        List<Long> deliveryIds = deliveries.stream().map(Delivery::getId).toList();
        Set<Long> snapshotIds = deliveryIds.isEmpty() ? Set.of() : recipientSnapshotRepository
            .findAllByDeliveryIdIn(deliveryIds).stream()
            .map(snapshot -> snapshot.getDeliveryId()).collect(Collectors.toSet());
        Set<Long> delayedIds = deliveryIds.isEmpty() ? Set.of() : delayRepository
            .findAllByDeliveryIdIn(deliveryIds).stream()
            .map(delay -> delay.getDelivery().getId()).collect(Collectors.toSet());

        return new GroupData(
            groupBy(deliveries, delivery -> delivery.getDeliveryGroup().getId())
            , groupBy(assignments, assignment -> assignment.getDeliveryGroup().getId())
            , groupBy(items, item -> item.getAssignment().getDeliveryGroup().getId())
            , groupBy(issues, issue -> issue.getAssignment().getDeliveryGroup().getId())
            , snapshotIds
            , delayedIds
        );
    }

    private AdminDeliveryGroupListItemResponse toListItem(DeliveryGroup group, GroupData data) {
        List<Delivery> deliveries = data.deliveriesByGroup().getOrDefault(group.getId(), List.of());
        List<DeliveryAssignment> assignments = data.assignmentsByGroup().getOrDefault(group.getId(), List.of());
        List<DeliveryAssignmentItem> items = data.itemsByGroup().getOrDefault(group.getId(), List.of());
        Map<Long, DeliveryAssignment> activeOwners = activeOwnerByDelivery(items);
        long unacknowledged = assignments.stream().filter(DeliveryAssignment::isAssigned).count();
        long unresolvedIssues = data.issuesByGroup().getOrDefault(group.getId(), List.of()).stream()
            .filter(issue -> issue.getResolution() == null).count();
        long delayed = deliveries.stream().filter(d -> data.delayedDeliveryIds().contains(d.getId())).count();
        long failed = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.FAILED).count();

        return new AdminDeliveryGroupListItemResponse(
            group.getId(), group.getDeliveryDate(), group.getSlot().getCode(), group.getStatus()
            , deliveries.size(), activeOwners.size(), deliveries.size() - activeOwners.size()
            , Math.toIntExact(unacknowledged), Math.toIntExact(unresolvedIssues)
            , Math.toIntExact(delayed), Math.toIntExact(failed)
            , toOffset(group.getAutoAssignmentCompletedAt())
            , toOffset(group.getActualStartedAt()), toOffset(group.getActualFinishedAt())
        );
    }

    private AdminDeliveryGroupDetailResponse.Assignment toAssignment(
        DeliveryAssignment assignment
        , List<DeliveryAssignmentItem> items
    ) {
        List<DeliveryAssignmentItem> mine = items.stream()
            .filter(item -> item.getAssignment().getId().equals(assignment.getId())).toList();
        int quantity = mine.stream().map(DeliveryAssignmentItem::getDelivery)
            .mapToInt(Delivery::getLunchboxQuantity).sum();
        return new AdminDeliveryGroupDetailResponse.Assignment(
            assignment.getId(), assignment.getRider().getId(), assignment.getStatus()
            , mine.size(), quantity
        );
    }

    private AdminDeliveryGroupDetailResponse.Issue toIssue(DeliveryAssignmentIssue issue) {
        return new AdminDeliveryGroupDetailResponse.Issue(
            issue.getId()
            , issue.getAssignment().getId()
            , issue.getIssueCode()
            , issue.getIssueDetail()
            , issue.getReportedBy()
            , toOffset(issue.getReportedAt())
            , issue.getResolution()
            , issue.getResolvedBy()
            , toOffset(issue.getResolvedAt())
        );
    }

    private AdminDeliveryGroupDetailResponse.ConfirmationReadiness readiness(
        DeliveryGroup group
        , List<Delivery> deliveries
        , List<DeliveryAssignment> assignments
        , List<DeliveryAssignmentItem> items
        , GroupData data
    ) {
        List<DeliveryAssignment> activeAssignments = assignments.stream()
            .filter(assignment -> assignment.getStatus().isActive()).toList();
        boolean allAcknowledged = activeAssignments.stream().allMatch(assignment ->
            assignment.getStatus() == DeliveryAssignmentStatus.ACKNOWLEDGED
                || assignment.getStatus() == DeliveryAssignmentStatus.CONFIRMED
        );
        boolean hasIssue = data.issuesByGroup().getOrDefault(group.getId(), List.of()).stream()
            .anyMatch(issue -> issue.getResolution() == null);
        boolean capacityValid = isCapacityValid(items);
        boolean ridersEligible = items.stream()
            .filter(item -> item.getAssignment().getStatus().isActive())
            .allMatch(item -> eligibilityService.isEligible(
                item.getAssignment().getRider()
                , group.getDeliveryDate()
                , group.getSlot().getCode()
                , item.getDelivery().getDeliveryAreaCode()
            ));

        return new AdminDeliveryGroupDetailResponse.ConfirmationReadiness(
            !deliveries.isEmpty()
            , deliveries.stream().allMatch(delivery -> delivery.getStatus() == DeliveryStatus.READY)
            , hasExactlyOneActiveAssignmentPerDelivery(deliveries, items)
            , !activeAssignments.isEmpty() && allAcknowledged
            , hasIssue
            , !activeAssignments.isEmpty() && ridersEligible
            , capacityValid
            , !deliveries.isEmpty() && deliveries.stream()
                .allMatch(delivery -> data.snapshotDeliveryIds().contains(delivery.getId()))
        );
    }

    boolean hasExactlyOneActiveAssignmentPerDelivery(
        List<Delivery> deliveries
        , List<DeliveryAssignmentItem> items
    ) {
        Map<Long, Long> activeAssignmentCountByDelivery = items.stream()
            .filter(item -> item.getAssignment().getStatus().isActive())
            .collect(Collectors.groupingBy(
                item -> item.getDelivery().getId()
                , Collectors.counting()
            ));

        return !deliveries.isEmpty()
            && deliveries.stream().allMatch(
                delivery -> activeAssignmentCountByDelivery.getOrDefault(
                    delivery.getId(), 0L
                ) == 1L
            );
    }

    boolean isCapacityValid(List<DeliveryAssignmentItem> items) {
        Map<Long, RiderLoad> loadByRider = new HashMap<>();
        items.stream()
            .filter(item -> item.getAssignment().getStatus().isActive())
            .forEach(item -> loadByRider
                .computeIfAbsent(
                    item.getAssignment().getRider().getId()
                    , ignored -> new RiderLoad()
                )
                .add(item.getDelivery().getLunchboxQuantity())
            );

        return loadByRider.values().stream().allMatch(load ->
            load.stopCount <= DeliveryAssignmentCapacity.MAX_VISIT_COUNT
                && load.lunchboxQuantity <= DeliveryAssignmentCapacity.MAX_LUNCHBOX_QUANTITY
        );
    }

    private OffsetDateTime confirmationTime(
        DeliveryGroup group
        , int lunchHour
        , int lunchMinute
        , int dinnerHour
        , int dinnerMinute
    ) {
        java.time.LocalTime time = group.getSlot().getCode() == DeliverySlotCode.LUNCH
            ? java.time.LocalTime.of(lunchHour, lunchMinute)
            : java.time.LocalTime.of(dinnerHour, dinnerMinute);
        return java.time.LocalDateTime.of(group.getDeliveryDate(), time)
            .atZone(KST).toOffsetDateTime();
    }

    private Map<Long, DeliveryAssignment> activeOwnerByDelivery(List<DeliveryAssignmentItem> items) {
        Map<Long, DeliveryAssignment> result = new HashMap<>();
        for (DeliveryAssignmentItem item : items) {
            if (item.getAssignment().getStatus().isActive()) {
                result.putIfAbsent(item.getDelivery().getId(), item.getAssignment());
            }
        }
        return result;
    }

    private AdminDeliveryDetailResponse.StatusHistory toStatusHistory(DeliveryStatusHistory history) {
        return new AdminDeliveryDetailResponse.StatusHistory(
            history.getFromStatus(), history.getToStatus(), history.getChangedBy()
            , toOffset(history.getChangedAt())
        );
    }

    private AdminDeliveryDetailResponse.AssignmentHistory toAssignmentHistory(DeliveryAssignment assignment) {
        return new AdminDeliveryDetailResponse.AssignmentHistory(
            assignment.getId(), assignment.getRider().getId(), assignment.getStatus()
            , toOffset(assignment.getAssignedAt()), toOffset(assignment.getAcknowledgedAt())
        );
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atZone(KST).toOffsetDateTime();
    }

    private <T> Map<Long, List<T>> groupBy(
        Collection<T> values
        , java.util.function.Function<T, Long> keyMapper
    ) {
        return values.stream().collect(Collectors.groupingBy(keyMapper));
    }

    private record CompletionValues(
        ActualHandoffType handoffType, String storageLocation
    ) {
    }

    private record FailureValues(
        DeliveryFailureCode failureCode, String failureDetail
    ) {
    }

    private record GroupData(
        Map<Long, List<Delivery>> deliveriesByGroup
        , Map<Long, List<DeliveryAssignment>> assignmentsByGroup
        , Map<Long, List<DeliveryAssignmentItem>> itemsByGroup
        , Map<Long, List<DeliveryAssignmentIssue>> issuesByGroup
        , Set<Long> snapshotDeliveryIds
        , Set<Long> delayedDeliveryIds
    ) {
        static GroupData empty() {
            return new GroupData(Map.of(), Map.of(), Map.of(), Map.of(), Set.of(), Set.of());
        }
    }

    private static final class RiderLoad {
        private int stopCount;
        private int lunchboxQuantity;

        private void add(Integer quantity) {
            stopCount++;
            lunchboxQuantity += quantity;
        }
    }
}
