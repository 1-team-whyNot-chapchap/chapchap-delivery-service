package com.chapchap.delivery.domain.assignment.controller;

import com.chapchap.delivery.domain.assignment.request.AdminAssignmentIssueReassignRequest;
import com.chapchap.delivery.domain.assignment.request.AdminAssignmentIssueRejectRequest;
import com.chapchap.delivery.domain.assignment.service.AdminAssignmentIssueReassignService;
import com.chapchap.delivery.domain.assignment.service.AdminAssignmentIssueRejectService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
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
@RequestMapping("/api/admin/assignment-issues")
public class AdminAssignmentIssueController {

    private final AdminAssignmentIssueRejectService adminAssignmentIssueRejectService;
    private final AdminAssignmentIssueReassignService adminAssignmentIssueReassignService;

    @PostMapping("/{issueId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
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
