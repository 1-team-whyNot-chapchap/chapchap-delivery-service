package com.chapchap.delivery.domain.rider.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RiderDeliveryAreaCreateRequest(
    @NotBlank
    @Size(max = 50)
    String deliveryAreaCode

    , @NotNull
    LocalDate effectiveFrom

    , LocalDate effectiveTo

    , @NotNull
    Boolean isActive
) {
}