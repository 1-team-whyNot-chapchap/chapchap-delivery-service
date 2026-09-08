package com.chapchap.delivery.domain.delivery.controller;

import com.chapchap.delivery.domain.delivery.constant.AdminDeliveryOperationType;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryOperationCountsResponse;
import com.chapchap.delivery.domain.delivery.response.AdminDeliveryOperationListResponse;
import com.chapchap.delivery.domain.delivery.service.AdminDeliveryOperationQueryService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Delivery Operations", description = "관리자가 확인해야 할 도시락 배송 운영 항목을 조회합니다.")
public class AdminDeliveryOperationController {
    private final AdminDeliveryOperationQueryService queryService;

    @GetMapping("/counts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Operation Counts", description = "배송일과 시간대별 도시락 배송 운영 항목 건수를 유형별로 조회합니다.")
    @ApiErrorCodes
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
    @Operation(summary = "Get Delivery Operations", description = "운영 유형, 배송일과 시간대 조건으로 관리자가 확인할 도시락 배송 목록을 조회합니다.")
    @ApiErrorCodes
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
