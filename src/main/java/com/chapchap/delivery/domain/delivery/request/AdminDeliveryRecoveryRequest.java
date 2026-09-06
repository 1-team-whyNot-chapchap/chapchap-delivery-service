package com.chapchap.delivery.domain.delivery.request;

import com.chapchap.delivery.domain.delivery.constant.DeliveryAdminRecoveryReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRecoveryResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminDeliveryRecoveryRequest(
    @NotNull DeliveryRecoveryResult recoveryResult
    , @NotNull DeliveryAdminRecoveryReason reasonCode
    , @Size(max = 500) String reasonDetail
    , @NotNull Long actualRiderId
    , @Valid RiderDeliveryCompletionRequest completion
    , @Valid RiderDeliveryFailureRequest failure
) {
}
