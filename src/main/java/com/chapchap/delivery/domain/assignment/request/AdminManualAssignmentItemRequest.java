package com.chapchap.delivery.domain.assignment.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminManualAssignmentItemRequest(
    @NotNull @Positive Long riderId
    , @NotEmpty List<
        @NotBlank
        @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )
        String
    > deliveryIds
    , boolean areaException
    , @Size(max = 32) String reasonCode
    , @Size(max = 500) String reasonDetail
) {
}
