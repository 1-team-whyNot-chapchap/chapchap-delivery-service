package com.chapchap.delivery.domain.rider.request;

import com.chapchap.delivery.domain.rider.constant.RiderLeaveSlotType;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RiderLeaveRequestCreateRequest(
    @NotNull LocalDate leaveDate
    , @NotNull RiderLeaveSlotType leaveSlot
    , @NotNull RiderLeaveType leaveType
    , @Size(max = 255) String reasonDetail
) {
}
