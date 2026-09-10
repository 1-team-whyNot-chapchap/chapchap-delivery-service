package com.chapchap.delivery.domain.assignment.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.ASSIGNMENT_CONDITION_NOT_MET;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_ASSIGNMENT_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_CAPACITY_EXCEEDED;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_GROUP_CONFIRMATION_CONDITION_NOT_MET;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_GROUP_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_GROUP_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_ASSIGNMENT_ISSUE_REASON;
import static com.chapchap.delivery.global.exception.ErrorCode.OTHER_REASON_DETAIL_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_NOT_FOUND;

import com.chapchap.delivery.domain.assignment.response.DeliveryGroupConfirmationResponse;
import com.chapchap.delivery.domain.assignment.response.ManualAssignmentsResponse;
import com.chapchap.delivery.domain.assignment.response.AdminRiderCandidateListResponse;
import com.chapchap.delivery.domain.assignment.request.AdminManualAssignmentsRequest;
import com.chapchap.delivery.domain.assignment.service.AdminAutoAssignmentService;
import com.chapchap.delivery.domain.assignment.service.AdminDeliveryGroupConfirmationService;
import com.chapchap.delivery.domain.assignment.service.AdminManualAssignmentService;
import com.chapchap.delivery.domain.assignment.service.AdminRiderCandidateQueryService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryGroupDetailResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryGroupListResponse;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/delivery-groups")
@Tag(name = "Admin Delivery Group", description = "같은 배송일과 시간대의 도시락 배송 그룹, 기사 배정과 최종 확정을 관리합니다.")
public class AdminDeliveryGroupController {
    private final AdminDeliveryGroupConfirmationService adminDeliveryGroupConfirmationService;
    private final AdminAutoAssignmentService adminAutoAssignmentService;
    private final AdminManualAssignmentService adminManualAssignmentService;
    private final AdminRiderCandidateQueryService adminRiderCandidateQueryService;
    private final AdminDeliveryQueryService adminDeliveryQueryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Delivery Groups", description = "배송일, 시간대, 상태 조건으로 도시락 배송 그룹을 조회합니다.")
    @ApiErrorCodes
    public ApiResponse<AdminDeliveryGroupListResponse> getDeliveryGroups(
        @AuthenticationPrincipal AuthenticatedUser user
        , @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate
        , @RequestParam(required = false) DeliverySlotCode deliverySlot
        , @RequestParam(required = false) DeliveryGroupStatus status
        , Pageable pageable
    ) {
        return ApiResponse.success(
            adminDeliveryQueryService.getDeliveryGroups(
                user.userId(), user.role(), deliveryDate, deliverySlot, status, pageable
            )
        );
    }

    @GetMapping("/{deliveryGroupId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Delivery Group", description = "도시락 배송 그룹의 기사 배정 현황과 고객별 배송 대상을 상세 조회합니다.")
    @ApiErrorCodes(DELIVERY_GROUP_NOT_FOUND)
    public ApiResponse<AdminDeliveryGroupDetailResponse> getDeliveryGroup(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable Long deliveryGroupId
    ) {
        return ApiResponse.success(
            adminDeliveryQueryService.getDeliveryGroup(
                user.userId(), user.role(), deliveryGroupId
            )
        );
    }

    @GetMapping("/{deliveryGroupId}/rider-candidates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get Rider Candidates"
        , description = "배송 그룹의 미배정 배송 또는 지정 배정 건을 담당할 수 있는 기사 후보를 조회합니다."
    )
    @ApiErrorCodes({DELIVERY_GROUP_NOT_FOUND, DELIVERY_ASSIGNMENT_NOT_FOUND})
    public ApiResponse<AdminRiderCandidateListResponse> getRiderCandidates(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable Long deliveryGroupId
        , @RequestParam(required = false) Long assignmentId
    ) {
        return ApiResponse.success(
            adminRiderCandidateQueryService.getCandidates(
                user.userId(), user.role(), deliveryGroupId, assignmentId
            )
        );
    }

    @PostMapping("/{deliveryGroupId}/auto-assignment")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run Auto Assignment", description = "배송 그룹의 미배정 도시락 배송을 가용 기사에게 자동 배정합니다.")
    @ApiErrorCodes({
        DELIVERY_GROUP_NOT_FOUND,
        DELIVERY_GROUP_STATE_CONFLICT,
        DELIVERY_ASSIGNMENT_STATE_CONFLICT,
        ASSIGNMENT_CONDITION_NOT_MET,
        DELIVERY_CAPACITY_EXCEEDED
    })
    public ApiResponse<Boolean> runAutoAssignment(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable Long deliveryGroupId
    ) {
        return ApiResponse.success(
            adminAutoAssignmentService.assign(
                authenticatedUser.userId()
                , authenticatedUser.role()
                , deliveryGroupId
            )
        );
    }

    @PostMapping("/{deliveryGroupId}/manual-assignments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Manual Assignments", description = "관리자가 선택한 기사에게 도시락 배송 대상을 수동 배정합니다.")
    @ApiErrorCodes({
        DELIVERY_GROUP_NOT_FOUND,
        RIDER_NOT_FOUND,
        DELIVERY_GROUP_STATE_CONFLICT,
        DELIVERY_ASSIGNMENT_STATE_CONFLICT,
        ASSIGNMENT_CONDITION_NOT_MET,
        DELIVERY_CAPACITY_EXCEEDED,
        INVALID_ASSIGNMENT_ISSUE_REASON,
        OTHER_REASON_DETAIL_REQUIRED
    })
    public ResponseEntity<ApiResponse<ManualAssignmentsResponse>> createManualAssignments(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable Long deliveryGroupId
        , @Valid @RequestBody AdminManualAssignmentsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    adminManualAssignmentService.assign(
                        authenticatedUser.userId()
                        , authenticatedUser.role()
                        , deliveryGroupId
                        , request
                    )
                )
            );
    }

    @PostMapping("/{deliveryGroupId}/confirmation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirm Delivery Group", description = "기사 배정 조건을 검증하고 도시락 배송 그룹을 최종 확정합니다.")
    @ApiErrorCodes({
        DELIVERY_GROUP_NOT_FOUND,
        DELIVERY_GROUP_CONFIRMATION_CONDITION_NOT_MET
    })
    public ApiResponse<DeliveryGroupConfirmationResponse> confirmDeliveryGroup(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
        , @PathVariable Long deliveryGroupId
    ) {
        DeliveryGroupConfirmationResponse response =
            adminDeliveryGroupConfirmationService.confirm(
                authenticatedUser.userId()
                , authenticatedUser.role()
                , deliveryGroupId
            );

        return ApiResponse.success(response);
    }
}
