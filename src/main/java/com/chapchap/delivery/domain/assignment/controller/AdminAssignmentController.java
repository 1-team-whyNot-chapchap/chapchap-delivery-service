package com.chapchap.delivery.domain.assignment.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.ASSIGNMENT_CONDITION_NOT_MET;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_ASSIGNMENT_STATE_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_CAPACITY_EXCEEDED;
import static com.chapchap.delivery.global.exception.ErrorCode.OTHER_REASON_DETAIL_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.RIDER_NOT_FOUND;

import com.chapchap.delivery.domain.assignment.request.AdminEmergencyRiderReplacementRequest;
import com.chapchap.delivery.domain.assignment.response.AdminEmergencyRiderReplacementResponse;
import com.chapchap.delivery.domain.assignment.service.AdminEmergencyRiderReplacementService;
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
@RequestMapping("/api/delivery/admin/assignments")
@Tag(name = "Admin Assignment", description = "도시락 배송 배정과 긴급 기사 교체를 관리합니다.")
public class AdminAssignmentController {
    private final AdminEmergencyRiderReplacementService replacementService;

    @PostMapping("/{assignmentId}/emergency-rider-replacement")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Emergency Rider Replacement", description = "확정된 도시락 배송 배정의 담당 기사를 긴급하게 교체합니다.")
    @ApiErrorCodes({
        DELIVERY_ASSIGNMENT_NOT_FOUND,
        RIDER_NOT_FOUND,
        OTHER_REASON_DETAIL_REQUIRED,
        DELIVERY_ASSIGNMENT_STATE_CONFLICT,
        ASSIGNMENT_CONDITION_NOT_MET,
        DELIVERY_CAPACITY_EXCEEDED
    })
    public ApiResponse<AdminEmergencyRiderReplacementResponse> replaceRider(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable Long assignmentId
        , @Valid @RequestBody AdminEmergencyRiderReplacementRequest request
    ) {
        return ApiResponse.success(
            replacementService.replace(
                user.userId(), user.role(), assignmentId, request
            )
        );
    }
}
