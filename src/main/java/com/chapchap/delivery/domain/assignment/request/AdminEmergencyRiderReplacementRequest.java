package com.chapchap.delivery.domain.assignment.request;

import com.chapchap.delivery.domain.assignment.constant.EmergencyRiderReplacementReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminEmergencyRiderReplacementRequest(
    @NotNull @Positive Long newRiderId
    , @NotNull EmergencyRiderReplacementReason reasonCode
    , @Size(max = 500) String reasonDetail
) {
}
