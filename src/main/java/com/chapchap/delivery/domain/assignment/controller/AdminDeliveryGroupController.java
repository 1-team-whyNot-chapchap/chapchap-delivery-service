package com.chapchap.delivery.domain.assignment.controller;

import com.chapchap.delivery.domain.assignment.response.DeliveryGroupConfirmationResponse;
import com.chapchap.delivery.domain.assignment.response.ManualAssignmentsResponse;
import com.chapchap.delivery.domain.assignment.request.AdminManualAssignmentsRequest;
import com.chapchap.delivery.domain.assignment.service.AdminAutoAssignmentService;
import com.chapchap.delivery.domain.assignment.service.AdminDeliveryGroupConfirmationService;
import com.chapchap.delivery.domain.assignment.service.AdminManualAssignmentService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import com.chapchap.delivery.domain.delivery.constant.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryGroupDetailResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryGroupListResponse;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryQueryService;
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
public class AdminDeliveryGroupController {
    private final AdminDeliveryGroupConfirmationService adminDeliveryGroupConfirmationService;
    private final AdminAutoAssignmentService adminAutoAssignmentService;
    private final AdminManualAssignmentService adminManualAssignmentService;
    private final AdminDeliveryQueryService adminDeliveryQueryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
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
