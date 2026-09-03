package com.chapchap.delivery.domain.rider.response;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.constant.RiderScheduleExceptionReason;
import com.chapchap.delivery.domain.rider.entity.RiderScheduleException;

import java.time.LocalDate;

public record RiderScheduleExceptionResponse(
    Long exceptionId
    , Long riderId
    , LocalDate scheduleDate
    , DeliverySlotCode deliverySlot
    , Boolean isWorking
    , RiderScheduleExceptionReason reasonCode
    , String reasonDetail
    , Long version
) {
    public static RiderScheduleExceptionResponse from(
        RiderScheduleException scheduleException
    ) {
        return new RiderScheduleExceptionResponse(
            scheduleException.getId()
            , scheduleException.getRider().getId()
            , scheduleException.getScheduleDate()
            , scheduleException.getSlot().getCode()
            , scheduleException.getIsWorking()
            , scheduleException.getReasonCode()
            , scheduleException.getReasonDetail()
            , scheduleException.getVersion()
        );
    }
}