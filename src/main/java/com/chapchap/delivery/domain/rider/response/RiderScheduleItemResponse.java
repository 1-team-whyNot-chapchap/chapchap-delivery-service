package com.chapchap.delivery.domain.rider.response;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleSource;

import java.time.LocalDate;

public record RiderScheduleItemResponse(
    LocalDate date
    , DeliverySlotCode deliverySlot
    , Boolean isWorking
    , RiderScheduleSource source
) {
}