package com.chapchap.delivery.domain.assignment.request;

import com.chapchap.delivery.domain.assignment.constant.AssignmentReassignmentReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminAssignmentIssueReassignRequest(
    @NotNull
    @Positive
    Long newRiderId

    , @NotNull
    AssignmentReassignmentReason reasonCode

    , @Size(max = 500)
    String reasonDetail
) {
}
