package com.chapchap.delivery.domain.assignment.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminManualAssignmentsRequest(
    @NotEmpty List<@Valid AdminManualAssignmentItemRequest> assignments
) {
}
