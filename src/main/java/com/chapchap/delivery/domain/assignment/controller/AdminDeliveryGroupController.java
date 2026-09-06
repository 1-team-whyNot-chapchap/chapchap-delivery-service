package com.chapchap.delivery.domain.assignment.controller;

import com.chapchap.delivery.domain.assignment.response.DeliveryGroupConfirmationResponse;
import com.chapchap.delivery.domain.assignment.response.ManualAssignmentsResponse;
import com.chapchap.delivery.domain.assignment.request.AdminManualAssignmentsRequest;
import com.chapchap.delivery.domain.assignment.service.AdminAutoAssignmentService;
import com.chapchap.delivery.domain.assignment.service.AdminDeliveryGroupConfirmationService;
import com.chapchap.delivery.domain.assignment.service.AdminManualAssignmentService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/delivery-groups")
public class AdminDeliveryGroupController {
    private final AdminDeliveryGroupConfirmationService adminDeliveryGroupConfirmationService;
    private final AdminAutoAssignmentService adminAutoAssignmentService;
    private final AdminManualAssignmentService adminManualAssignmentService;

    @PostMapping("/{deliveryGroupId}/auto-assignment")
    @PreAuthorize("hasRole('ADMIN')")
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
