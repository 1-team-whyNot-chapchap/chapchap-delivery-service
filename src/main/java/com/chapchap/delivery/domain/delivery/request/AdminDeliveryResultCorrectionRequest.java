package com.chapchap.delivery.domain.delivery.request;

import com.chapchap.delivery.domain.delivery.constant.DeliveryResultCorrectionReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminDeliveryResultCorrectionRequest(
    @NotEmpty @Size(max = 2) List<@NotNull @Valid Change> changes
    , @NotNull DeliveryResultCorrectionReason reasonCode
    , @Size(max = 500) String reasonDetail
) {
    public record Change(
        @NotBlank @Size(max = 50) String fieldName
        , @Size(max = 500) String afterValue
    ) {
    }
}
