package com.chapchap.delivery.domain.rider.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_ASSIGNMENT_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_GROUP_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_HANDOFF_INFO_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_ASSIGNMENT_ISSUE_REASON;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_DELIVERY_FAILURE_REASON;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_DELIVERY_PHOTO_INFO;
import static com.chapchap.delivery.global.exception.ErrorCode.OTHER_REASON_DETAIL_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_NOT_FOUND;

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
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryStartResponse;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryCompletionRequest;
import com.chapchap.delivery.domain.delivery.request.RiderDeliveryFailureRequest;
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryCompletionResponse;
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryFailureResponse;
import com.chapchap.delivery.domain.delivery.service.RiderDeliveryCompletionService;
import com.chapchap.delivery.domain.delivery.service.RiderDeliveryFailureService;
import com.chapchap.delivery.domain.delivery.request.RiderEmergencyDeliveryFailureRequest;
import com.chapchap.delivery.domain.delivery.response.RiderEmergencyDeliveryFailureResponse;
import com.chapchap.delivery.domain.delivery.service.RiderEmergencyDeliveryFailureService;
import com.chapchap.delivery.domain.delivery.service.RiderDeliveryStartService;
import com.chapchap.delivery.domain.rider.response.RiderScheduleResponse;
import com.chapchap.delivery.domain.rider.service.RiderScheduleService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/rider")
@Tag(name = "Rider Delivery", description = "기사가 본인의 일정과 도시락 배송 배정을 확인하고 배송을 수행합니다.")
public class RiderMeController {

    private final RiderScheduleService riderScheduleService;
    private final RiderAssignmentAcknowledgementService riderAssignmentAcknowledgementService;
    private final RiderAssignmentIssueService riderAssignmentIssueService;
    private final RiderAssignmentQueryService riderAssignmentQueryService;
    private final RiderAssignmentDetailService riderAssignmentDetailService;
    private final RiderDeliveryStartService riderDeliveryStartService;
    private final RiderDeliveryCompletionService riderDeliveryCompletionService;
    private final RiderDeliveryFailureService riderDeliveryFailureService;
    private final RiderEmergencyDeliveryFailureService riderEmergencyDeliveryFailureService;

    @GetMapping("/schedules")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Get My Schedules", description = "조회 기간의 주간 일정과 예외 일정을 합산해 본인의 도시락 배송 근무 일정을 조회합니다.")
    @ApiErrorCodes(RIDER_NOT_FOUND)
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
    @Operation(summary = "Get My Assignments", description = "배송일, 시간대와 상태 조건으로 본인의 도시락 배송 배정을 조회합니다.")
    @ApiErrorCodes
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
    @Operation(summary = "Get My Assignment Detail", description = "본인에게 배정된 도시락 배송 묶음과 고객별 배송 대상의 상세 정보를 조회합니다.")
    @ApiErrorCodes(DELIVERY_ASSIGNMENT_NOT_FOUND)
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
    @Operation(summary = "Acknowledge Assignment", description = "본인에게 배정된 도시락 배송 내용을 확인하고 수행 의사를 확정합니다.")
    @ApiErrorCodes({DELIVERY_ASSIGNMENT_NOT_FOUND, DELIVERY_ASSIGNMENT_STATE_CONFLICT})
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
    @Operation(summary = "Report Assignment Issue", description = "도시락 배송 배정을 수행하기 어려운 사유를 관리자에게 보고합니다.")
    @ApiErrorCodes({
        DELIVERY_ASSIGNMENT_NOT_FOUND,
        INVALID_ASSIGNMENT_ISSUE_REASON,
        OTHER_REASON_DETAIL_REQUIRED,
        DELIVERY_ASSIGNMENT_STATE_CONFLICT
    })
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

    @PostMapping("/deliveries/{deliveryId}/start")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Start Delivery", description = "본인에게 배정된 고객의 도시락 배송을 시작합니다.")
    @ApiErrorCodes({DELIVERY_NOT_FOUND, DELIVERY_GROUP_STATE_CONFLICT, DELIVERY_STATE_CONFLICT})
    public ApiResponse<RiderDeliveryStartResponse> startDelivery(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable String deliveryId
    ) {
        RiderDeliveryStartResponse response =
            riderDeliveryStartService.start(
                authenticatedUser.userId()
                , deliveryId
            );

        return ApiResponse.success(response);
    }

    @PostMapping(
        value = "/deliveries/{deliveryId}/complete"
        , consumes = "multipart/form-data"
    )
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Complete Delivery", description = "도시락 수령 방식과 배송 완료 근거를 기록하고, 필요한 경우 완료 사진을 함께 업로드합니다.")
    @ApiErrorCodes({
        DELIVERY_NOT_FOUND,
        INVALID_DELIVERY_PHOTO_INFO,
        DELIVERY_HANDOFF_INFO_REQUIRED,
        DELIVERY_STATE_CONFLICT
    })
    public ApiResponse<RiderDeliveryCompletionResponse> completeDelivery(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable String deliveryId
        , @Valid @RequestPart("request") RiderDeliveryCompletionRequest request
        , @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        return ApiResponse.success(
            riderDeliveryCompletionService.complete(
                authenticatedUser.userId()
                , deliveryId
                , request
                , photo
            )
        );
    }

    @PostMapping("/deliveries/{deliveryId}/fail")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Fail Delivery", description = "도시락 배송을 완료하지 못한 사유와 상세 내용을 기록합니다.")
    @ApiErrorCodes({DELIVERY_NOT_FOUND, INVALID_DELIVERY_FAILURE_REASON, DELIVERY_STATE_CONFLICT})
    public ApiResponse<RiderDeliveryFailureResponse> failDelivery(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable String deliveryId
        , @Valid @RequestBody RiderDeliveryFailureRequest request
    ) {
        return ApiResponse.success(
            riderDeliveryFailureService.fail(
                authenticatedUser.userId()
                , deliveryId
                , request
            )
        );
    }

    @PostMapping("/assignments/{assignmentId}/emergency-failures")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Fail Remaining Deliveries", description = "긴급 상황으로 수행할 수 없는 배정 내 남은 도시락 배송을 일괄 실패 처리합니다.")
    @ApiErrorCodes({
        DELIVERY_ASSIGNMENT_NOT_FOUND,
        INVALID_DELIVERY_FAILURE_REASON,
        DELIVERY_ASSIGNMENT_STATE_CONFLICT,
        DELIVERY_STATE_CONFLICT
    })
    public ApiResponse<RiderEmergencyDeliveryFailureResponse> failRemainingDeliveries(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable Long assignmentId
        , @Valid @RequestBody RiderEmergencyDeliveryFailureRequest request
    ) {
        return ApiResponse.success(
            riderEmergencyDeliveryFailureService.failRemaining(
                authenticatedUser.userId()
                , assignmentId
                , request
            )
        );
    }
}
