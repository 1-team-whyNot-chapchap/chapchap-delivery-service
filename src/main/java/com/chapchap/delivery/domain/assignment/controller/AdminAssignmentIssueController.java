package com.chapchap.delivery.domain.assignment.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.ASSIGNMENT_CONDITION_NOT_MET;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_ASSIGNMENT_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_CAPACITY_EXCEEDED;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_ASSIGNMENT_ISSUE_REASON;
import static com.chapchap.delivery.global.exception.ErrorCode.OTHER_REASON_DETAIL_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_NOT_FOUND;

import com.chapchap.delivery.domain.assignment.request.AdminAssignmentIssueReassignRequest;
import com.chapchap.delivery.domain.assignment.request.AdminAssignmentIssueRejectRequest;
import com.chapchap.delivery.domain.assignment.service.AdminAssignmentIssueReassignService;
import com.chapchap.delivery.domain.assignment.service.AdminAssignmentIssueRejectService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/assignment-issues")
@Tag(name = "Admin Assignment Issue", description = "기사가 보고한 도시락 배송 배정 이슈를 처리합니다.")
public class AdminAssignmentIssueController {

    private final AdminAssignmentIssueRejectService adminAssignmentIssueRejectService;
    private final AdminAssignmentIssueReassignService adminAssignmentIssueReassignService;

    @PostMapping("/{issueId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject Assignment Issue", description = "기사의 배송 배정 이슈를 반려하고 처리 사유를 기록합니다.")
    @ApiErrorCodes({
        DELIVERY_ASSIGNMENT_NOT_FOUND,
        INVALID_ASSIGNMENT_ISSUE_REASON,
        DELIVERY_ASSIGNMENT_STATE_CONFLICT
    })
    public ApiResponse<Void> rejectAssignmentIssue(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable Long issueId
        , @Valid @RequestBody AdminAssignmentIssueRejectRequest request
    ) {
        adminAssignmentIssueRejectService.rejectIssue(
            authenticatedUser.userId()
            , authenticatedUser.role()
            , issueId
            , request.reasonDetail()
        );

        return ApiResponse.success(null);
    }

    @PostMapping("/{issueId}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reassign Assignment Issue", description = "배송 배정 이슈를 승인하고 새로운 기사에게 배송을 재배정합니다.")
    @ApiErrorCodes({
        DELIVERY_ASSIGNMENT_NOT_FOUND,
        RIDER_NOT_FOUND,
        INVALID_ASSIGNMENT_ISSUE_REASON,
        OTHER_REASON_DETAIL_REQUIRED,
        DELIVERY_ASSIGNMENT_STATE_CONFLICT,
        ASSIGNMENT_CONDITION_NOT_MET,
        DELIVERY_CAPACITY_EXCEEDED
    })
    public ApiResponse<Void> reassignAssignmentIssue(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable Long issueId
        , @Valid @RequestBody AdminAssignmentIssueReassignRequest request
    ) {
        adminAssignmentIssueReassignService.reassignIssue(
            authenticatedUser.userId()
            , authenticatedUser.role()
            , issueId
            , request.newRiderId()
            , request.reasonCode()
            , request.reasonDetail()
        );

        return ApiResponse.success(null);
    }

}
