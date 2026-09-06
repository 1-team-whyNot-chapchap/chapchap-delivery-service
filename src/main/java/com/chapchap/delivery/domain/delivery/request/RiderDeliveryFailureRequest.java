package com.chapchap.delivery.domain.delivery.request;

import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record RiderDeliveryFailureRequest(
    @NotNull
    DeliveryFailureStage failureStage

    , @NotNull
    DeliveryFailureCode failureCode

    , @Size(max = 500)
    String failureDetail

    , OffsetDateTime contactAttemptedAt

    , @Size(max = 30)
    String contactResult

    , @NotNull
    Boolean itemRecovered

    , OffsetDateTime recoveredAt
) {
}