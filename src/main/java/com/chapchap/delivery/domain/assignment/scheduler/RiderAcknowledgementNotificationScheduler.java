package com.chapchap.delivery.domain.assignment.scheduler;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentAcknowledgementReminderStage;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.assignment.service.RiderAcknowledgementNotificationService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.global.config.DeliveryVerificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RiderAcknowledgementNotificationScheduler {

    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final RiderAcknowledgementNotificationService riderAcknowledgementNotificationService;
    private final DeliveryVerificationProperties deliveryVerificationProperties;

    @Scheduled(
        cron = "${app.scheduler.rider-acknowledgement.lunch-open-cron:0 0 7 * * *}"
        , zone = "Asia/Seoul"
    )
    public void publishLunchAcknowledgementOpened() {
        publishAcknowledgementOpened(
            DeliverySlotCode.LUNCH
        );
    }

    @Scheduled(
        cron = "${app.scheduler.rider-acknowledgement.lunch-first-reminder-cron:0 0 8 * * *}"
        , zone = "Asia/Seoul"
    )
    public void publishLunchFirstReminder() {
        publishAcknowledgementReminder(
            DeliverySlotCode.LUNCH
            , DeliveryAssignmentAcknowledgementReminderStage.FIRST
        );
    }

    @Scheduled(
        cron = "${app.scheduler.rider-acknowledgement.lunch-final-reminder-cron:0 30 8 * * *}"
        , zone = "Asia/Seoul"
    )
    public void publishLunchFinalReminder() {
        publishAcknowledgementReminder(
            DeliverySlotCode.LUNCH
            , DeliveryAssignmentAcknowledgementReminderStage.FINAL
        );
    }

    @Scheduled(
        cron = "${app.scheduler.rider-acknowledgement.dinner-open-cron:0 0 13 * * *}"
        , zone = "Asia/Seoul"
    )
    public void publishDinnerAcknowledgementOpened() {
        publishAcknowledgementOpened(
            DeliverySlotCode.DINNER
        );
    }

    @Scheduled(
        cron = "${app.scheduler.rider-acknowledgement.dinner-first-reminder-cron:0 0 14 * * *}"
        , zone = "Asia/Seoul"
    )
    public void publishDinnerFirstReminder() {
        publishAcknowledgementReminder(
            DeliverySlotCode.DINNER
            , DeliveryAssignmentAcknowledgementReminderStage.FIRST
        );
    }

    @Scheduled(
        cron = "${app.scheduler.rider-acknowledgement.dinner-final-reminder-cron:0 30 14 * * *}"
        , zone = "Asia/Seoul"
    )
    public void publishDinnerFinalReminder() {
        publishAcknowledgementReminder(
            DeliverySlotCode.DINNER
            , DeliveryAssignmentAcknowledgementReminderStage.FINAL
        );
    }

    private void publishAcknowledgementOpened(
        DeliverySlotCode deliverySlot
    ) {
        List<Long> assignmentIds =
            findPendingAssignmentIds(
                deliverySlot
            );

        for (Long assignmentId : assignmentIds) {
            riderAcknowledgementNotificationService.publishOpened(
                assignmentId
            );
        }
    }

    private void publishAcknowledgementReminder(
        DeliverySlotCode deliverySlot
        , DeliveryAssignmentAcknowledgementReminderStage reminderStage
    ) {
        List<Long> assignmentIds =
            findPendingAssignmentIds(
                deliverySlot
            );

        for (Long assignmentId : assignmentIds) {
            riderAcknowledgementNotificationService.publishReminder(
                assignmentId
                , reminderStage
            );
        }
    }

    private List<Long> findPendingAssignmentIds(
        DeliverySlotCode deliverySlot
    ) {
        LocalDate deliveryDate =
            LocalDate.now(KST);
        deliveryDate = deliveryDate.plusDays(
            deliveryVerificationProperties
                .riderAcknowledgementTargetDateOffsetDays()
        );

        return deliveryAssignmentRepository.findIdsForAcknowledgementPending(
            deliveryDate
            , deliverySlot
            , DeliveryAssignmentStatus.ASSIGNED
        );
    }
}
