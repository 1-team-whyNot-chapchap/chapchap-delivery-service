package com.chapchap.delivery.domain.assignment.constant;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;

import java.time.LocalTime;

public final class DeliveryAssignmentAcknowledgementTime {

    public static final LocalTime LUNCH =
        LocalTime.of(7, 0);

    public static final LocalTime DINNER =
        LocalTime.of(13, 0);

    private static final LocalTime LUNCH_RESPONSE_DEADLINE =
        LocalTime.of(9, 0);

    private static final LocalTime DINNER_RESPONSE_DEADLINE =
        LocalTime.of(15, 0);

    private DeliveryAssignmentAcknowledgementTime() {
    }

    public static LocalTime getStart(
        DeliverySlotCode deliverySlot
    ) {
        return switch (deliverySlot) {
            case LUNCH ->
                LUNCH;

            case DINNER ->
                DINNER;
        };
    }

    public static LocalTime getResponseDeadline(
        DeliverySlotCode deliverySlot
    ) {
        return switch (deliverySlot) {
            case LUNCH ->
                LUNCH_RESPONSE_DEADLINE;

            case DINNER ->
                DINNER_RESPONSE_DEADLINE;
        };
    }
}