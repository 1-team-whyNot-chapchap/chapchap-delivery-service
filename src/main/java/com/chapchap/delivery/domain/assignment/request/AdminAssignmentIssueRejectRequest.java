package com.chapchap.delivery.domain.assignment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAssignmentIssueRejectRequest(
    @NotBlank
    @Size(max = 500)
    String reasonDetail
) {
}