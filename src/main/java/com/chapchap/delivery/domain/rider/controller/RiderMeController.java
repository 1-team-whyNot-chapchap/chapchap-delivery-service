package com.chapchap.delivery.domain.rider.controller;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignmentIssue;
import com.chapchap.delivery.domain.assignment.request.RiderAssignmentIssueRequest;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentAcknowledgementResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentDetailResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentIssueResponse;
import com.chapchap.delivery.domain.assignment.response.RiderAssignmentListResponse;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentAcknowledgementService;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentDetailService;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentIssueService;
import com.chapchap.delivery.domain.assignment.service.RiderAssignmentQueryService;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.rider.response.RiderScheduleResponse;
import com.chapchap.delivery.domain.rider.service.RiderScheduleService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
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
@RequestMapping("/api/rider/me")
public class RiderMeController {

    private final RiderScheduleService riderScheduleService;
    private final RiderAssignmentAcknowledgementService riderAssignmentAcknowledgementService;
    private final RiderAssignmentIssueService riderAssignmentIssueService;
    private final RiderAssignmentQueryService riderAssignmentQueryService;
    private final RiderAssignmentDetailService riderAssignmentDetailService;

    @GetMapping("/schedules")
    @PreAuthorize("hasRole('RIDER')")
    public ApiResponse<RiderScheduleResponse> getMySchedules(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom
        , @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        RiderScheduleResponse response =
            riderScheduleService.getMySchedules(
                authenticatedUser.userId()
                , authenticatedUser.role()
                , dateFrom
                , dateTo
            );

        return ApiResponse.success(response);
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasRole('RIDER')")
    public ApiResponse<RiderAssignmentListResponse> getMyAssignments(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate
        , @RequestParam(required = false) DeliverySlotCode deliverySlot
        , @RequestParam(required = false) DeliveryAssignmentStatus status
        , Pageable pageable
    ) {
        RiderAssignmentListResponse response =
            riderAssignmentQueryService.getMyAssignments(
                authenticatedUser.userId()
                , deliveryDate
                , deliverySlot
                , status
                , pageable
            );

        return ApiResponse.success(response);
    }

    @GetMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('RIDER')")
    public ApiResponse<RiderAssignmentDetailResponse> getMyAssignmentDetail(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable Long assignmentId
    ) {
        RiderAssignmentDetailResponse response =
            riderAssignmentDetailService.getMyAssignmentDetail(
                authenticatedUser.userId()
                , assignmentId
            );

        return ApiResponse.success(response);
    }

    @PostMapping("/assignments/{assignmentId}/acknowledgement")
    @PreAuthorize("hasRole('RIDER')")
    public ApiResponse<RiderAssignmentAcknowledgementResponse> acknowledgeAssignment(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable Long assignmentId
    ) {
        DeliveryAssignment assignment =
            riderAssignmentAcknowledgementService.acknowledge(
                authenticatedUser.userId()
                , assignmentId
            );

        RiderAssignmentAcknowledgementResponse response =
            RiderAssignmentAcknowledgementResponse.from(
                assignment
            );

        return ApiResponse.success(response);
    }

    @PostMapping("/assignments/{assignmentId}/issues")
    @PreAuthorize("hasRole('RIDER')")
    public ApiResponse<RiderAssignmentIssueResponse> reportAssignmentIssue(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable Long assignmentId
        , @Valid @RequestBody RiderAssignmentIssueRequest request
    ) {
        DeliveryAssignmentIssue issue =
            riderAssignmentIssueService.reportIssue(
                authenticatedUser.userId()
                , assignmentId
                , request.issueCode()
                , request.issueDetail()
            );

        RiderAssignmentIssueResponse response =
            RiderAssignmentIssueResponse.from(
                issue
            );

        return ApiResponse.success(response);
    }
}
