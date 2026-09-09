package com.chapchap.delivery.domain.rider.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_LEAVE_REQUEST_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_LEAVE_REQUEST_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_LEAVE_REVIEW_DEADLINE_PASSED;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_LEAVE_SCHEDULE_CONFLICT;

import com.chapchap.delivery.domain.rider.constant.RiderLeaveRequestStatus;
import com.chapchap.delivery.domain.rider.constant.RiderLeaveSlotType;
import com.chapchap.delivery.domain.rider.request.RiderLeaveRejectionRequest;
import com.chapchap.delivery.domain.rider.response.AdminRiderLeaveRequestListResponse;
import com.chapchap.delivery.domain.rider.response.RiderLeaveRequestResponse;
import com.chapchap.delivery.domain.rider.service.AdminRiderLeaveReviewService;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/rider-leave-requests")
@Tag(name = "Admin Rider", description = "도시락 배송 기사의 배달 활성 상태, 근무 일정과 담당 지역을 관리합니다.")
public class AdminRiderLeaveRequestController {
    private final AdminRiderLeaveReviewService reviewService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Rider Leave Requests", description = "관리자가 기사 휴무 신청을 기사, 날짜, 시간대와 처리 상태 조건으로 조회합니다.")
    @ApiErrorCodes
    public ApiResponse<AdminRiderLeaveRequestListResponse> getLeaveRequests(
        @AuthenticationPrincipal AuthenticatedUser user, @RequestParam(required = false) Long riderId,
        @RequestParam(required = false) RiderLeaveRequestStatus status,
        @RequestParam(required = false) RiderLeaveSlotType leaveSlot,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leaveDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leaveDateTo,
        Pageable pageable
    ) {
        return ApiResponse.success(reviewService.getAll(user.userId(), user.role(), riderId, status, leaveSlot, leaveDateFrom, leaveDateTo, pageable));
    }

    @GetMapping("/{leaveRequestId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Rider Leave Request", description = "관리자가 기사 휴무 신청 한 건과 신청·심사 정보를 상세 조회합니다.")
    @ApiErrorCodes(RIDER_LEAVE_REQUEST_NOT_FOUND)
    public ApiResponse<RiderLeaveRequestResponse> getLeaveRequest(
        @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long leaveRequestId
    ) {
        return ApiResponse.success(reviewService.getDetail(user.userId(), user.role(), leaveRequestId));
    }

    @PostMapping("/{leaveRequestId}/approval")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve Rider Leave Request", description = "배송일 기준 2일 전까지 기사 휴무 신청을 승인하고 해당 시간대를 근무 불가 일정으로 반영합니다.")
    @ApiErrorCodes({RIDER_LEAVE_REQUEST_NOT_FOUND, RIDER_LEAVE_REQUEST_STATE_CONFLICT, RIDER_LEAVE_REVIEW_DEADLINE_PASSED, RIDER_LEAVE_SCHEDULE_CONFLICT})
    public ApiResponse<RiderLeaveRequestResponse> approve(
        @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long leaveRequestId
    ) {
        return ApiResponse.success(reviewService.approve(user.userId(), user.role(), leaveRequestId));
    }

    @PostMapping("/{leaveRequestId}/rejection")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject Rider Leave Request", description = "배송일 기준 2일 전까지 기사 휴무 신청을 반려하고 반려 사유를 기록합니다.")
    @ApiErrorCodes({RIDER_LEAVE_REQUEST_NOT_FOUND, RIDER_LEAVE_REQUEST_STATE_CONFLICT, RIDER_LEAVE_REVIEW_DEADLINE_PASSED})
    public ApiResponse<RiderLeaveRequestResponse> reject(
        @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long leaveRequestId,
        @Valid @RequestBody RiderLeaveRejectionRequest request
    ) {
        return ApiResponse.success(reviewService.reject(user.userId(), user.role(), leaveRequestId, request.reasonDetail()));
    }
}
