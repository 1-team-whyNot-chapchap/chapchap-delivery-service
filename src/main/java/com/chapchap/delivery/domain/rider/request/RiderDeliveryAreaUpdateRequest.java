package com.chapchap.delivery.domain.rider.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RiderDeliveryAreaUpdateRequest(
    LocalDate effectiveTo
    , @NotNull Boolean isActive
) {
}