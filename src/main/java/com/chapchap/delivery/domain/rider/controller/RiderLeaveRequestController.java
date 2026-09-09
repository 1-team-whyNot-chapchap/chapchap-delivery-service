package com.chapchap.delivery.domain.rider.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_LEAVE_REASON_DETAIL_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_LEAVE_REQUEST_DEADLINE_PASSED;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_LEAVE_REQUEST_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_LEAVE_REQUEST_OVERLAP;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_NOT_FOUND;

import com.chapchap.delivery.domain.rider.request.RiderLeaveRequestCreateRequest;
import com.chapchap.delivery.domain.rider.response.RiderLeaveRequestResponse;
import com.chapchap.delivery.domain.rider.service.RiderLeaveRequestService;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/rider/leave-requests")
@Tag(name = "Rider Delivery", description = "기사가 본인의 일정과 도시락 배송 배정을 확인하고 배송을 수행합니다.")
public class RiderLeaveRequestController {
    private final RiderLeaveRequestService riderLeaveRequestService;

    @PostMapping
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Create Leave Request", description = "기사가 배송일 기준 5일 전까지 종일·점심·저녁 단위의 휴무를 신청합니다.")
    @ApiErrorCodes({RIDER_NOT_FOUND, RIDER_LEAVE_REQUEST_DEADLINE_PASSED, RIDER_LEAVE_REQUEST_OVERLAP, RIDER_LEAVE_REASON_DETAIL_REQUIRED})
    public ApiResponse<RiderLeaveRequestResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @Valid @RequestBody RiderLeaveRequestCreateRequest request) {
        return ApiResponse.success(riderLeaveRequestService.create(user.userId(), user.role(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Get My Leave Requests", description = "기사가 본인의 휴무 신청과 현재 승인·반려 상태를 조회합니다.")
    @ApiErrorCodes
    public ApiResponse<List<RiderLeaveRequestResponse>> getMine(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(riderLeaveRequestService.getMine(user.userId(), user.role()));
    }

    @GetMapping("/{leaveRequestId}")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Get My Leave Request", description = "기사가 본인의 휴무 신청 한 건을 상세 조회합니다.")
    @ApiErrorCodes(RIDER_LEAVE_REQUEST_NOT_FOUND)
    public ApiResponse<RiderLeaveRequestResponse> getMineDetail(@AuthenticationPrincipal AuthenticatedUser user,
                                                                  @PathVariable Long leaveRequestId) {
        return ApiResponse.success(riderLeaveRequestService.getMineDetail(user.userId(), user.role(), leaveRequestId));
    }
}
