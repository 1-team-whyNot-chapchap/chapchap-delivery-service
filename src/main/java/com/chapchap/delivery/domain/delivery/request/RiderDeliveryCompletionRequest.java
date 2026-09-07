package com.chapchap.delivery.domain.delivery.request;

import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.ContactResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record RiderDeliveryCompletionRequest(
    @NotNull ActualHandoffType actualHandoffType
    , @Size(max = 100) String storageLocation
    , OffsetDateTime contactAttemptedAt
    , ContactResult contactResult
) {
}
