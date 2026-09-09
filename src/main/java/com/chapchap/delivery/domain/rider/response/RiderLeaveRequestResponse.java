package com.chapchap.delivery.domain.rider.response;

import com.chapchap.delivery.domain.rider.constant.RiderLeaveRequestStatus;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveSlotType;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveType;
import com.chapchap.delivery.domain.rider.entity.RiderLeaveRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record RiderLeaveRequestResponse(
    Long leaveRequestId
    , Long riderId
    , LocalDate leaveDate
    , RiderLeaveSlotType leaveSlot
    , RiderLeaveType leaveType
    , String reasonDetail
    , RiderLeaveRequestStatus status
    , OffsetDateTime requestedAt
    , Long reviewedBy
    , OffsetDateTime reviewedAt
    , String reviewReasonDetail
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static RiderLeaveRequestResponse from(RiderLeaveRequest request) {
        return new RiderLeaveRequestResponse(
            request.getId(), request.getRider().getId(), request.getLeaveDate(), request.getLeaveSlot(),
            request.getLeaveType(), request.getReasonDetail(), request.getStatus(),
            request.getRequestedAt().atZone(KST).toOffsetDateTime(), request.getReviewedBy(),
            request.getReviewedAt() == null ? null : request.getReviewedAt().atZone(KST).toOffsetDateTime(),
            request.getReviewReasonDetail()
        );
    }
}
