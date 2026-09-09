package com.chapchap.delivery.global.kafka.event;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DeliveryOperationNotificationData(
    String notificationType
    , String recipientType
    , Long recipientUserId
    , String referenceType
    , String referenceId
    , LocalDate deliveryDate
    , String deliverySlot
    , String businessKey
    , String reminderStage
    , String actionReason
    , OffsetDateTime responseDeadline
) {
}