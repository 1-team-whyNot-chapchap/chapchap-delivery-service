package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentItem;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DeliveryExecutionSupport {
    private final DeliveryGroupStatusHistoryRepository groupHistoryRepository;

    public DeliveryExecutionSupport(
        DeliveryGroupStatusHistoryRepository groupHistoryRepository
    ) {
        this.groupHistoryRepository = groupHistoryRepository;
    }

    public void validateCurrentConfirmedAssignment(
        Rider rider
        , Delivery delivery
        , List<DeliveryAssignmentItem> items
    ) {
        boolean assigned = items.stream()
            .anyMatch(
                item ->
                    item.getDelivery().getId().equals(delivery.getId())
                        && item.getAssignment().getRider().getId().equals(rider.getId())
                        && item.getAssignment().getStatus()
                        == DeliveryAssignmentStatus.CONFIRMED
            );

        if (!assigned) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    public void recalculateGroup(
        DeliveryGroup group
        , List<Delivery> deliveries
        , LocalDateTime changedAt
    ) {
        if (deliveries.isEmpty()) {
            throw new DeliveryStateConflictException();
        }

        boolean hasDelivering = deliveries.stream()
            .anyMatch(
                delivery ->
                    delivery.getStatus() == DeliveryStatus.DELIVERING
            );

        if (hasDelivering) {
            startGroupExecution(
                group
                , changedAt
            );

            return;
        }

        boolean hasReady = deliveries.stream()
            .anyMatch(
                delivery ->
                    delivery.getStatus() == DeliveryStatus.READY
            );

        if (hasReady) {
            return;
        }

        long deliveredCount = deliveries.stream()
            .filter(
                delivery ->
                    delivery.getStatus() == DeliveryStatus.DELIVERED
            )
            .count();

        long failedCount = deliveries.stream()
            .filter(
                delivery ->
                    delivery.getStatus() == DeliveryStatus.FAILED
            )
            .count();

        DeliveryGroupStatus finalStatus =
            determineFinalStatus(
                deliveries.size()
                , deliveredCount
                , failedCount
            );

        finishGroupExecution(
            group
            , finalStatus
            , changedAt
        );
    }

    private void startGroupExecution(
        DeliveryGroup group
        , LocalDateTime changedAt
    ) {
        DeliveryGroupStatus previousStatus =
            group.getStatus();

        boolean changed =
            group.startExecution(changedAt);

        if (!changed) {
            return;
        }

        saveGroupHistory(
            group
            , previousStatus
            , DeliveryGroupStatus.IN_PROGRESS
            , changedAt
        );
    }

    private void finishGroupExecution(
        DeliveryGroup group
        , DeliveryGroupStatus finalStatus
        , LocalDateTime changedAt
    ) {
        DeliveryGroupStatus previousStatus =
            group.getStatus();

        boolean changed =
            group.finishExecution(
                finalStatus
                , changedAt
            );

        if (!changed) {
            return;
        }

        saveGroupHistory(
            group
            , previousStatus
            , finalStatus
            , changedAt
        );
    }

    private DeliveryGroupStatus determineFinalStatus(
        int totalCount
        , long deliveredCount
        , long failedCount
    ) {
        if (deliveredCount == totalCount) {
            return DeliveryGroupStatus.COMPLETED;
        }

        if (failedCount == totalCount) {
            return DeliveryGroupStatus.FAILED;
        }

        if (
            deliveredCount > 0
                && failedCount > 0
                && deliveredCount + failedCount == totalCount
        ) {
            return DeliveryGroupStatus.COMPLETED_WITH_FAILURE;
        }

        throw new DeliveryStateConflictException();
    }

    private void saveGroupHistory(
        DeliveryGroup group
        , DeliveryGroupStatus fromStatus
        , DeliveryGroupStatus toStatus
        , LocalDateTime changedAt
    ) {
        groupHistoryRepository.save(
            new DeliveryGroupStatusHistory(
                group
                , fromStatus
                , toStatus
                , null
                , DeliveryGroupChangedByType.SYSTEM
                , changedAt
            )
        );
    }
}
