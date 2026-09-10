package com.chapchap.delivery.domain.riderlocation.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RiderLocationUpdateRequest(
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    @NotNull @DecimalMin("0.0") BigDecimal accuracy,
    @NotNull OffsetDateTime capturedAt
) {
}
