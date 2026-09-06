package com.chapchap.delivery.domain.delivery.request;

import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record RiderDeliveryCompletionRequest(
    @NotNull ActualHandoffType actualHandoffType
    , @Size(max = 100) String storageLocation
    , OffsetDateTime contactAttemptedAt
    , @Size(max = 30) String contactResult
) {
}
