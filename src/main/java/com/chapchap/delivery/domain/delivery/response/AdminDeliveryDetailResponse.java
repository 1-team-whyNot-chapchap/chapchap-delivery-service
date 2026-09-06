package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryProcessedByType;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.RequestHandoffType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminDeliveryDetailResponse(
    String deliveryId
    , String orderId
    , Long customerId
    , Long deliveryGroupId
    , LocalDate deliveryDate
    , DeliverySlotCode deliverySlot
    , DeliveryStatus status
    , Integer deliveryVersion
    , Integer lunchboxQuantity
    , String menuId
    , String menuName
    , RequestHandoffType requestedHandoffType
    , Delay delay
    , Completion completion
    , Failure failure
    , List<StatusHistory> statusHistories
    , List<AssignmentHistory> assignmentHistories
) {
    public record Delay(Integer delayMinutes, OffsetDateTime detectedAt) {
    }

    public record Completion(
        ActualHandoffType actualHandoffType
        , String storageLocation
        , OffsetDateTime contactAttemptedAt
        , String contactResult
        , Long processedBy
        , DeliveryProcessedByType processedByType
        , String adminReasonCode
        , String adminReasonDetail
        , OffsetDateTime completedAt
        , boolean hasCompletionPhoto
    ) {
    }

    public record Failure(
        DeliveryFailureStage failureStage
        , DeliveryFailureCode failureCode
        , String failureDetail
        , OffsetDateTime contactAttemptedAt
        , String contactResult
        , Boolean itemRecovered
        , OffsetDateTime recoveredAt
        , Long processedBy
        , DeliveryProcessedByType processedByType
        , String adminReasonCode
        , String adminReasonDetail
        , OffsetDateTime failedAt
    ) {
    }

    public record StatusHistory(
        DeliveryStatus fromStatus
        , DeliveryStatus toStatus
        , Long changedBy
        , OffsetDateTime changedAt
    ) {
    }

    public record AssignmentHistory(
        Long assignmentId
        , Long riderId
        , DeliveryAssignmentStatus status
        , OffsetDateTime assignedAt
        , OffsetDateTime acknowledgedAt
    ) {
    }
}
