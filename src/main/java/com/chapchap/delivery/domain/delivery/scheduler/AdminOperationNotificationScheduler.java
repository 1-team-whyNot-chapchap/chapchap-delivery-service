package com.chapchap.delivery.domain.delivery.scheduler;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentIssueRepository;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.domain.delivery.service.AdminOperationNotificationService;
import com.chapchap.delivery.global.kafka.constant.DeliveryOperationNotificationBusinessKey;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminOperationNotificationScheduler {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String OPERATION_EVENT_TYPE =
        "DELIVERY_OPERATION_NOTIFICATION_REQUESTED";

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentIssueRepository issueRepository;
    private final IntegrationEventRecordRepository eventRecordRepository;
    private final AdminOperationNotificationService notificationService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void publishPendingNotifications() {
        LocalDateTime now = LocalDateTime.now(KST);
        publishLateOrders(now);
        publishAssignmentIssues();
        publishAcknowledgementOverdue(now);
        publishUnresolvedDeliveries(now);
        publishEventFailures();
    }

    void publishLateOrders(LocalDateTime now) {
        LocalDate deliveryDate = now.toLocalDate().plusDays(1);
        LocalDateTime cutoff = LocalDateTime.of(now.toLocalDate(), LocalTime.of(16, 10));
        deliveryRepository.findLateOrdersForNotification(deliveryDate, cutoff)
            .forEach(delivery -> publishDelivery(
                "ADMIN_LATE_ORDER_REVIEW", delivery,
                DeliveryOperationNotificationBusinessKey.adminLateOrder(
                    delivery.getDeliveryPublicId()
                ), null
            ));
    }

    void publishAssignmentIssues() {
        for (DeliveryAssignmentIssue issue : issueRepository.findAllUnresolvedForNotification()) {
            var group = issue.getAssignment().getDeliveryGroup();
            notificationService.publish(
                "ADMIN_ASSIGNMENT_ACTION_REQUIRED", "ASSIGNMENT_ISSUE",
                String.valueOf(issue.getId()), group.getDeliveryDate(),
                group.getSlot().getCode().name(),
                DeliveryOperationNotificationBusinessKey.adminAssignmentAction(
                    "ISSUE", issue.getId(), "ISSUE_REPORTED"
                ), "ISSUE_REPORTED"
            );
        }
    }

    void publishAcknowledgementOverdue(LocalDateTime now) {
        LocalTime lunch = LocalTime.of(9, 0);
        LocalTime dinner = LocalTime.of(15, 0);
        if (!now.toLocalTime().isBefore(lunch)) {
            publishAcknowledgementOverdue(now.toLocalDate(), DeliverySlotCode.LUNCH);
        }
        if (!now.toLocalTime().isBefore(dinner)) {
            publishAcknowledgementOverdue(now.toLocalDate(), DeliverySlotCode.DINNER);
        }
    }

    private void publishAcknowledgementOverdue(LocalDate date, DeliverySlotCode slot) {
        assignmentRepository.findIdsForAcknowledgementPending(
            date, slot, DeliveryAssignmentStatus.ASSIGNED
        ).forEach(assignmentId -> notificationService.publish(
            "ADMIN_ASSIGNMENT_ACTION_REQUIRED", "DELIVERY_ASSIGNMENT",
            String.valueOf(assignmentId), date, slot.name(),
            DeliveryOperationNotificationBusinessKey.adminAssignmentAction(
                "ASSIGNMENT", assignmentId, "ACK_OVERDUE"
            ), "ACK_OVERDUE"
        ));
    }

    void publishUnresolvedDeliveries(LocalDateTime now) {
        publishUnresolved(now, DeliverySlotCode.LUNCH, LocalTime.of(13, 30));
        publishUnresolved(now, DeliverySlotCode.DINNER, LocalTime.of(19, 30));
    }

    private void publishUnresolved(
        LocalDateTime now, DeliverySlotCode slot, LocalTime deadline
    ) {
        if (now.toLocalTime().isBefore(deadline)) {
            return;
        }
        deliveryRepository.findUnresolvedByDeliveryDateAndSlot(
            now.toLocalDate(), slot, List.of(DeliveryStatus.READY, DeliveryStatus.DELIVERING)
        ).forEach(delivery -> publishDelivery(
            "ADMIN_UNRESOLVED_DELIVERY", delivery,
            DeliveryOperationNotificationBusinessKey.adminUnresolvedDelivery(
                delivery.getDeliveryPublicId()
            ), null
        ));
    }

    void publishEventFailures() {
        eventRecordRepository.findFailedForAdminNotification(
            IntegrationEventDirection.PUBLISH, IntegrationEventStatus.FAILED,
            OPERATION_EVENT_TYPE
        ).forEach(record -> notificationService.publish(
            "ADMIN_EVENT_PUBLISH_FAILED", "INTEGRATION_EVENT_RECORD",
            String.valueOf(record.getId()), null, null,
            DeliveryOperationNotificationBusinessKey.adminEventPublishFailed(record.getId()), null
        ));
    }

    private void publishDelivery(
        String type, Delivery delivery, String businessKey, String actionReason
    ) {
        var group = delivery.getDeliveryGroup();
        notificationService.publish(
            type, "DELIVERY_TARGET", delivery.getDeliveryPublicId(),
            group.getDeliveryDate(), group.getSlot().getCode().name(),
            businessKey, actionReason
        );
    }
}
