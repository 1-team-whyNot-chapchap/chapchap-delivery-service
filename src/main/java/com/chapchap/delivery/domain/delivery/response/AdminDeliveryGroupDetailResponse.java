package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueCode;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentIssueResolution;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminDeliveryGroupDetailResponse(
    Long deliveryGroupId
    , LocalDate deliveryDate
    , DeliverySlotCode deliverySlot
    , DeliveryGroupStatus status
    , OffsetDateTime autoAssignmentCompletedAt
    , OffsetDateTime actualStartedAt
    , OffsetDateTime actualFinishedAt
    , OffsetDateTime confirmationTargetAt
    , OffsetDateTime confirmationDeadlineAt
    , ConfirmationReadiness confirmationReadiness
    , List<Assignment> assignments
    , List<Issue> issues
    , List<DeliveryItem> deliveries
) {
    public record ConfirmationReadiness(
        boolean hasAtLeastOneDelivery
        , boolean allDeliveriesReady
        , boolean allDeliveriesAssignedOnce
        , boolean allAssignmentsAcknowledged
        , boolean hasUnresolvedIssue
        , boolean allRidersEligible
        , boolean capacityValid
        , boolean requiredDeliveryInformationComplete
    ) {
    }

    public record Assignment(
        Long assignmentId
        , Long riderId
        , DeliveryAssignmentStatus status
        , int stopCount
        , int lunchboxQuantity
    ) {
    }

    public record Issue(
        Long issueId
        , Long assignmentId
        , DeliveryAssignmentIssueCode issueCode
        , String issueDetail
        , Long reportedBy
        , OffsetDateTime reportedAt
        , DeliveryAssignmentIssueResolution resolution
        , Long resolvedBy
        , OffsetDateTime resolvedAt
    ) {
    }

    public record DeliveryItem(
        String deliveryId
        , Long customerId
        , DeliveryStatus status
        , Integer lunchboxQuantity
        , Long assignmentId
        , boolean delayed
    ) {
    }
}
