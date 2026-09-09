package com.chapchap.delivery.domain.rider.request;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RiderWeeklyScheduleCreateRequest(
    @NotNull
    @Min(1)
    @Max(7)
    Byte dayOfWeek

    , @NotNull
    DeliverySlotCode deliverySlot
) {
}