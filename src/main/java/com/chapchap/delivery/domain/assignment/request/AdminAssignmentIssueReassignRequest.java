package com.chapchap.delivery.domain.assignment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminAssignmentIssueReassignRequest(
    @NotNull
    @Positive
    Long newRiderId

    , @NotBlank
    @Size(max = 32)
    String reasonCode

    , @Size(max = 500)
    String reasonDetail
) {
}