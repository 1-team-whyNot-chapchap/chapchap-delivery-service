package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentAcknowledgementTime;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.AdminDeliveryOperationType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryOperationCountsResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryOperationItemResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryOperationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDeliveryOperationQueryService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;

    private final DeliveryAccessService accessService;
    private final DeliveryGroupRepository groupRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final IntegrationEventRecordRepository eventRecordRepository;

    @Transactional(readOnly = true)
    public AdminDeliveryOperationCountsResponse getCounts(
        Long actorId
        , UserRole role
        , LocalDate deliveryDate
        , DeliverySlotCode slotCode
    ) {
        accessService.validateAdminAccess(actorId, role);
        Map<AdminDeliveryOperationType, List<AdminDeliveryOperationItemResponse>> operations =
            operations(deliveryDate, slotCode, LocalDateTime.now(KST));
        return new AdminDeliveryOperationCountsResponse(
            count(operations, AdminDeliveryOperationType.AUTO_ASSIGNMENT_FINAL_FAILURE)
            , count(operations, AdminDeliveryOperationType.LATE_ORDER_REVIEW)
            , count(operations, AdminDeliveryOperationType.ACKNOWLEDGEMENT_OVERDUE)
            , count(operations, AdminDeliveryOperationType.UNRESOLVED_DELIVERY)
            , count(operations, AdminDeliveryOperationType.EVENT_PUBLISH_FAILED)
        );
    }

    @Transactional(readOnly = true)
    public AdminDeliveryOperationListResponse getOperations(
        Long actorId
        , UserRole role
        , AdminDeliveryOperationType type
        , LocalDate deliveryDate
        , DeliverySlotCode slotCode
        , Pageable pageable
    ) {
        accessService.validateAdminAccess(actorId, role);
        List<AdminDeliveryOperationItemResponse> items = new ArrayList<>(
            operations(deliveryDate, slotCode, LocalDateTime.now(KST))
                .getOrDefault(type, List.of())
        );
        items.sort(Comparator
            .comparing(
                AdminDeliveryOperationItemResponse::deliveryDate,
                Comparator.nullsLast(Comparator.reverseOrder())
            )
            .thenComparing(
                AdminDeliveryOperationItemResponse::deliveryGroupId,
                Comparator.nullsLast(Comparator.naturalOrder())
            )
            .thenComparing(item -> item.deliveryId() == null ? "" : item.deliveryId())
            .thenComparing(item -> item.assignmentId() == null ? 0L : item.assignmentId())
            .thenComparing(item ->
                item.integrationEventRecordId() == null ? 0L : item.integrationEventRecordId()
            ));

        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        int fromIndex = Math.min(pageable.getPageNumber() * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = items.isEmpty() ? 0 : (items.size() + size - 1) / size;
        return new AdminDeliveryOperationListResponse(
            List.copyOf(items.subList(fromIndex, toIndex))
            , pageable.getPageNumber()
            , size
            , items.size()
            , totalPages
            , pageable.getPageNumber() + 1 < totalPages
        );
    }

    Map<AdminDeliveryOperationType, List<AdminDeliveryOperationItemResponse>> operations(
        LocalDate deliveryDate
        , DeliverySlotCode slotCode
        , LocalDateTime now
    ) {
        List<DeliveryGroup> groups = groupRepository.findAllForOperations(deliveryDate, slotCode);
        List<Long> groupIds = groups.stream().map(DeliveryGroup::getId).toList();
        List<Delivery> deliveries = groupIds.isEmpty()
            ? List.of() : deliveryRepository.findAllByDeliveryGroupIdIn(groupIds);
        List<DeliveryAssignment> assignments = groupIds.isEmpty()
            ? List.of() : assignmentRepository.findAllByDeliveryGroupIdIn(groupIds);
        List<AdminDeliveryOperationItemResponse> result = new ArrayList<>();
        groups.stream().filter(group -> isAutoAssignmentFinalFailure(group, now))
            .map(group -> groupOperation(
                AdminDeliveryOperationType.AUTO_ASSIGNMENT_FINAL_FAILURE
                , group
                , LocalDateTime.of(group.getDeliveryDate().minusDays(1), LocalTime.of(17, 0))
            ))
            .forEach(result::add);
        deliveries.stream().filter(delivery -> isLateOrder(delivery))
            .map(delivery -> deliveryOperation(
                AdminDeliveryOperationType.LATE_ORDER_REVIEW, delivery, delivery.getCreatedAt()
            ))
            .forEach(result::add);
        assignments.stream().filter(assignment -> isAcknowledgementOverdue(assignment, now))
            .map(this::assignmentOperation)
            .forEach(result::add);
        deliveries.stream().filter(delivery -> isUnresolved(delivery, now))
            .map(delivery -> deliveryOperation(
                AdminDeliveryOperationType.UNRESOLVED_DELIVERY
                , delivery
                , unresolvedDeadline(delivery.getDeliveryGroup())
            ))
            .forEach(result::add);
        eventRecordRepository.findAllByDirectionAndStatusOrderByIdAsc(
            IntegrationEventDirection.PUBLISH, IntegrationEventStatus.FAILED
        ).stream().map(record -> new AdminDeliveryOperationItemResponse(
            AdminDeliveryOperationType.EVENT_PUBLISH_FAILED,
            null, null, null, null, null, null, null, null, null,
            toOffset(record.getLastAttemptedAt()), record.getId(), record.getEventType()
        )).forEach(result::add);

        return result.stream().collect(Collectors.groupingBy(AdminDeliveryOperationItemResponse::type));
    }

    private boolean isAutoAssignmentFinalFailure(DeliveryGroup group, LocalDateTime now) {
        return group.getDeliveryDate().equals(now.toLocalDate().plusDays(1))
            && now.isAfter(LocalDateTime.of(now.toLocalDate(), LocalTime.of(17, 0)))
            && group.getStatus() == DeliveryGroupStatus.WAITING_ASSIGNMENT
            && group.getAutoAssignmentCompletedAt() == null;
    }

    private boolean isLateOrder(Delivery delivery) {
        LocalDateTime createdAt = delivery.getCreatedAt();
        return createdAt != null && createdAt.isAfter(LocalDateTime.of(
            delivery.getDeliveryGroup().getDeliveryDate().minusDays(1)
            , LocalTime.of(16, 10)
        ));
    }

    private boolean isAcknowledgementOverdue(DeliveryAssignment assignment, LocalDateTime now) {
        DeliveryGroup group = assignment.getDeliveryGroup();
        LocalDateTime deadline = LocalDateTime.of(
            group.getDeliveryDate()
            , DeliveryAssignmentAcknowledgementTime.getResponseDeadline(group.getSlot().getCode())
        );
        return assignment.getStatus() == DeliveryAssignmentStatus.ASSIGNED
            && !now.isBefore(deadline);
    }

    private boolean isUnresolved(Delivery delivery, LocalDateTime now) {
        DeliveryGroup group = delivery.getDeliveryGroup();
        return (delivery.getStatus() == DeliveryStatus.READY
            || delivery.getStatus() == DeliveryStatus.DELIVERING)
            && !now.isBefore(unresolvedDeadline(group));
    }

    private LocalDateTime unresolvedDeadline(DeliveryGroup group) {
        LocalTime deadline = group.getSlot().getCode() == DeliverySlotCode.LUNCH
            ? LocalTime.of(13, 30) : LocalTime.of(19, 30);
        return LocalDateTime.of(group.getDeliveryDate(), deadline);
    }

    private AdminDeliveryOperationItemResponse groupOperation(
        AdminDeliveryOperationType type, DeliveryGroup group, LocalDateTime detectedAt
    ) {
        return new AdminDeliveryOperationItemResponse(
            type, group.getId(), null, null, null, group.getDeliveryDate(),
            group.getSlot().getCode(), group.getStatus(), null, null, toOffset(detectedAt),
            null, null
        );
    }

    private AdminDeliveryOperationItemResponse deliveryOperation(
        AdminDeliveryOperationType type, Delivery delivery, LocalDateTime detectedAt
    ) {
        DeliveryGroup group = delivery.getDeliveryGroup();
        return new AdminDeliveryOperationItemResponse(
            type, group.getId(), delivery.getDeliveryPublicId(), null, null,
            group.getDeliveryDate(), group.getSlot().getCode(), group.getStatus(),
            delivery.getStatus(), null, toOffset(detectedAt), null, null
        );
    }

    private AdminDeliveryOperationItemResponse assignmentOperation(DeliveryAssignment assignment) {
        DeliveryGroup group = assignment.getDeliveryGroup();
        LocalDateTime detectedAt = LocalDateTime.of(
            group.getDeliveryDate()
            , DeliveryAssignmentAcknowledgementTime.getResponseDeadline(group.getSlot().getCode())
        );
        return new AdminDeliveryOperationItemResponse(
            AdminDeliveryOperationType.ACKNOWLEDGEMENT_OVERDUE, group.getId(), null,
            assignment.getId(), assignment.getRider().getId(), group.getDeliveryDate(),
            group.getSlot().getCode(), group.getStatus(), null, assignment.getStatus(),
            toOffset(detectedAt), null, null
        );
    }

    private long count(
        Map<AdminDeliveryOperationType, List<AdminDeliveryOperationItemResponse>> operations
        , AdminDeliveryOperationType type
    ) {
        return operations.getOrDefault(type, List.of()).size();
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value.atZone(KST).toOffsetDateTime();
    }
}
