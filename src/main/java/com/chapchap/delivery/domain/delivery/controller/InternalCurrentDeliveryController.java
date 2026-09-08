package com.chapchap.delivery.domain.delivery.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.CURRENT_DELIVERY_DATA_CONFLICT;
import static com.chapchap.delivery.global.exception.ErrorCode.CURRENT_DELIVERY_NOT_FOUND;

import com.chapchap.delivery.domain.delivery.response.CurrentDeliveryResponse;
import com.chapchap.delivery.domain.delivery.service.CurrentDeliveryQueryService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/deliveries")
@Tag(name = "Internal Current Delivery", description = "Customer-Service와 Customer-AI가 고객의 현재 도시락 배송 상태를 조회합니다.")
public class InternalCurrentDeliveryController {
    private final CurrentDeliveryQueryService queryService;

    @GetMapping("/current")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get Current Delivery", description = "서비스 인증을 통과한 호출자가 고객의 현재 진행 중인 도시락 배송을 조회합니다.")
    @ApiErrorCodes({CURRENT_DELIVERY_NOT_FOUND, CURRENT_DELIVERY_DATA_CONFLICT})
    public ApiResponse<CurrentDeliveryResponse> getCurrent(
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(queryService.getCurrent(user.userId(), user.role()));
    }
}
