package com.chapchap.delivery.domain.delivery.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.COMPLETION_PHOTO_REQUIRED;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.INVALID_DELIVERY_INFO;

import com.chapchap.delivery.domain.delivery.response.DeliveryPhotoAccessResponse;
import com.chapchap.delivery.domain.delivery.response.CustomerDeliveryDetailResponse;
import com.chapchap.delivery.domain.delivery.response.CustomerDeliveryListResponse;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.service.CustomerDeliveryQueryService;
import com.chapchap.delivery.domain.delivery.service.DeliveryPhotoAccessService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Customer Delivery", description = "고객이 본인의 도시락 정기 구독 배송 현황과 완료 사진을 조회합니다.")
public class CustomerDeliveryController {
    private final DeliveryPhotoAccessService photoAccessService;
    private final CustomerDeliveryQueryService queryService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get My Deliveries", description = "배송일, 시간대와 상태 조건으로 본인의 도시락 정기 구독 배송 목록을 조회합니다.")
    @ApiErrorCodes
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
    @Operation(summary = "Get My Delivery", description = "본인의 도시락 정기 구독 배송 상태와 수령 정보를 상세 조회합니다.")
    @ApiErrorCodes({DELIVERY_NOT_FOUND, INVALID_DELIVERY_INFO})
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
    @Operation(summary = "Issue Completion Photo Access URL", description = "본인 도시락 배송의 현재 유효한 완료 사진에 대한 10분 만료 접근 URL을 발급합니다.")
    @ApiErrorCodes({DELIVERY_NOT_FOUND, COMPLETION_PHOTO_REQUIRED})
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
