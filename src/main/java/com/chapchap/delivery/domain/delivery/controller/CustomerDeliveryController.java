package com.chapchap.delivery.domain.delivery.controller;

import com.chapchap.delivery.domain.delivery.response.DeliveryPhotoAccessResponse;
import com.chapchap.delivery.domain.delivery.response.CustomerDeliveryDetailResponse;
import com.chapchap.delivery.domain.delivery.response.CustomerDeliveryListResponse;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.service.CustomerDeliveryQueryService;
import com.chapchap.delivery.domain.delivery.service.DeliveryPhotoAccessService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/customer/deliveries")
public class CustomerDeliveryController {
    private final DeliveryPhotoAccessService photoAccessService;
    private final CustomerDeliveryQueryService queryService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<CustomerDeliveryListResponse> getMyDeliveries(
        @AuthenticationPrincipal AuthenticatedUser user
        , @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDateFrom
        , @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDateTo
        , @RequestParam(required = false) DeliverySlotCode deliverySlot
        , @RequestParam(required = false) DeliveryStatus status
        , Pageable pageable
    ) {
        return ApiResponse.success(
            queryService.getMyDeliveries(
                user.userId()
                , user.role()
                , deliveryDateFrom
                , deliveryDateTo
                , deliverySlot
                , status
                , pageable
            )
        );
    }

    @GetMapping("/{deliveryId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<CustomerDeliveryDetailResponse> getMyDelivery(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable String deliveryId
    ) {
        return ApiResponse.success(
            queryService.getMyDelivery(user.userId(), user.role(), deliveryId)
        );
    }

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
