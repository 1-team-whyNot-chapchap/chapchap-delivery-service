package com.chapchap.delivery.domain.delivery.controller;

import com.chapchap.delivery.domain.delivery.constant.AdminDeliveryOperationType;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryOperationCountsResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryOperationListResponse;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryOperationQueryService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/delivery-operations")
public class AdminDeliveryOperationController {
    private final AdminDeliveryOperationQueryService queryService;

    @GetMapping("/counts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDeliveryOperationCountsResponse> getCounts(
        @AuthenticationPrincipal AuthenticatedUser user
        , @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate
        , @RequestParam(required = false) DeliverySlotCode deliverySlot
    ) {
        return ApiResponse.success(
            queryService.getCounts(user.userId(), user.role(), deliveryDate, deliverySlot)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDeliveryOperationListResponse> getOperations(
        @AuthenticationPrincipal AuthenticatedUser user
        , @RequestParam AdminDeliveryOperationType type
        , @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate
        , @RequestParam(required = false) DeliverySlotCode deliverySlot
        , Pageable pageable
    ) {
        return ApiResponse.success(
            queryService.getOperations(
                user.userId(), user.role(), type, deliveryDate, deliverySlot, pageable
            )
        );
    }
}
