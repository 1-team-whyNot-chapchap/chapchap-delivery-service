package com.chapchap.delivery.domain.assignment.request;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RiderAssignmentIssueRequest(
    @NotNull DeliveryAssignmentIssueCode issueCode
    , @Size(max = 500) String issueDetail
) {
}
