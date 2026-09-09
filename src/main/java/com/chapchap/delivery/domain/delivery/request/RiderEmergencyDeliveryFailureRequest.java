package com.chapchap.delivery.domain.delivery.request;

import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record RiderEmergencyDeliveryFailureRequest(
    @NotNull DeliveryFailureCode failureCode
    , @Size(max = 500) String failureDetail
    , @NotNull Boolean itemRecovered
    , OffsetDateTime recoveredAt
) {
}
