package com.chapchap.delivery.domain.rider.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RiderLeaveRejectionRequest(
    @NotBlank @Size(max = 500) String reasonDetail
) {
}
