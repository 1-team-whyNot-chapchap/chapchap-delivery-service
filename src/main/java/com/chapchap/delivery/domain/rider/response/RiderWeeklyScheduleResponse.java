package com.chapchap.delivery.domain.rider.response;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.entity.RiderWeeklySchedule;

public record RiderWeeklyScheduleResponse(
    Long scheduleId
    , Long riderId
    , Byte dayOfWeek
    , DeliverySlotCode deliverySlot
) {

    public static RiderWeeklyScheduleResponse from(
        RiderWeeklySchedule schedule
    ) {
        return new RiderWeeklyScheduleResponse(
            schedule.getId()
            , schedule.getRider().getId()
            , schedule.getDayOfWeek()
            , schedule.getSlot().getCode()
        );
    }
}