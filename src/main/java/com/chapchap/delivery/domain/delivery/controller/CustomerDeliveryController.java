package com.chapchap.delivery.domain.delivery.controller;

import com.chapchap.delivery.domain.delivery.response.DeliveryPhotoAccessResponse;
import com.chapchap.delivery.domain.delivery.service.DeliveryPhotoAccessService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/customer/deliveries")
public class CustomerDeliveryController {
    private final DeliveryPhotoAccessService photoAccessService;

    @PostMapping("/{deliveryId}/completion-photo/access")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<DeliveryPhotoAccessResponse> photoAccess(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
    ) {
        return ApiResponse.success(
            photoAccessService.forCustomer(
                user.userId()
                , user.role()
                , deliveryId
            )
        );
    }
}
