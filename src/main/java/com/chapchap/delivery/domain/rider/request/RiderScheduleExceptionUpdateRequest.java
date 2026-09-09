package com.chapchap.delivery.domain.rider.request;

import com.chapchap.delivery.domain.rider.constant.RiderScheduleExceptionReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RiderScheduleExceptionUpdateRequest(
    @NotNull Boolean isWorking
    , @NotNull RiderScheduleExceptionReason reasonCode
    , @Size(max = 255) String reasonDetail
    , @NotNull @PositiveOrZero Long version
) {
}