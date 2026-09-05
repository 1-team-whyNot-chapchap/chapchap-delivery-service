package com.chapchap.delivery.domain.assignment.response;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueCode;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record RiderAssignmentIssueResponse(
    Long issueId
    , Long assignmentId
    , DeliveryAssignmentStatus assignmentStatus
    , DeliveryAssignmentIssueCode issueCode
    , String issueDetail
    , OffsetDateTime reportedAt
) {
    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    public static RiderAssignmentIssueResponse from(
        DeliveryAssignmentIssue issue
    ) {
        return new RiderAssignmentIssueResponse(
            issue.getId()
            , issue.getAssignment().getId()
            , issue.getAssignment().getStatus()
            , issue.getIssueCode()
            , issue.getIssueDetail()
            , issue.getReportedAt()
            .atZone(KST)
            .toOffsetDateTime()
        );
    }
}