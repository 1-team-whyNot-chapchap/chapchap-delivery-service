package com.chapchap.delivery.domain.delivery.controller;

import com.chapchap.delivery.domain.delivery.response.CurrentDeliveryResponse;
import com.chapchap.delivery.domain.delivery.service.CurrentDeliveryQueryService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/deliveries")
public class InternalCurrentDeliveryController {
    private final CurrentDeliveryQueryService queryService;

    @GetMapping("/current")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<CurrentDeliveryResponse> getCurrent(
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(queryService.getCurrent(user.userId(), user.role()));
    }
}
