package com.chapchap.delivery.domain.rider.request;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleExceptionReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RiderScheduleExceptionCreateRequest(
    @NotNull
    LocalDate scheduleDate

    , @NotNull
    DeliverySlotCode deliverySlot

    , @NotNull
    Boolean isWorking

    , @NotNull
    RiderScheduleExceptionReason reasonCode

    , @Size(max = 255)
    String reasonDetail
) {
}