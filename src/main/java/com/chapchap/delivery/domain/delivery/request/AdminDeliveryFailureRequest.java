package com.chapchap.delivery.domain.delivery.request;

import com.chapchap.delivery.domain.delivery.constant.AdminDeliveryFailureReason;
import com.chapchap.delivery.domain.delivery.constant.ContactResult;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record AdminDeliveryFailureRequest(
    @NotNull DeliveryFailureStage failureStage
    , @NotNull DeliveryFailureCode failureCode
    , @Size(max = 500) String failureDetail
    , OffsetDateTime contactAttemptedAt
    , ContactResult contactResult
    , @NotNull Boolean itemRecovered
    , OffsetDateTime recoveredAt
    , @NotNull AdminDeliveryFailureReason adminReasonCode
    , @Size(max = 500) String adminReasonDetail
) {
}
