package com.chapchap.delivery.domain.rider.request;

import com.chapchap.delivery.domain.rider.constant.RiderDeliveryActiveReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RiderUpdateRequest(
    @NotNull
    Boolean isDeliveryActive

    , @NotNull
    @PositiveOrZero
    Long version

    , @NotNull
    RiderDeliveryActiveReason reasonCode

    , @Size(max = 500)
    String reasonDetail
) {
}