package com.chapchap.delivery.domain.delivery.controller;

import com.chapchap.delivery.domain.delivery.request.AdminDeliveryFailureRequest;
import com.chapchap.delivery.domain.delivery.request.AdminDeliveryRecoveryRequest;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryRecoveryResponse;
import com.chapchap.delivery.domain.delivery.response.DeliveryPhotoAccessResponse;
import com.chapchap.delivery.domain.delivery.response.RiderDeliveryFailureResponse;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryFailureService;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryRecoveryService;
import com.chapchap.delivery.domain.delivery.service.DeliveryPhotoAccessService;
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
@RequestMapping("/api/delivery/admin/deliveries")
public class AdminDeliveryController {
    private final AdminDeliveryFailureService failureService;
    private final DeliveryPhotoAccessService photoAccessService;
    private final AdminDeliveryRecoveryService recoveryService;

    @PostMapping("/{deliveryId}/completion-photo/access")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DeliveryPhotoAccessResponse> photoAccess(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
    ) {
        return ApiResponse.success(
            photoAccessService.forAdmin(
                user.userId()
                , user.role()
                , deliveryId
            )
        );
    }

    @PostMapping("/{deliveryId}/recovery")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDeliveryRecoveryResponse> recover(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
        , @Valid @RequestBody AdminDeliveryRecoveryRequest request
    ) {
        return ApiResponse.success(
            recoveryService.recover(
                user.userId()
                , user.role()
                , deliveryId
                , request
            )
        );
    }

    @PostMapping("/{deliveryId}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RiderDeliveryFailureResponse> fail(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
        , @Valid @RequestBody AdminDeliveryFailureRequest request
    ) {
        return ApiResponse.success(
            failureService.fail(user.userId(), user.role(), deliveryId, request)
        );
    }
}
