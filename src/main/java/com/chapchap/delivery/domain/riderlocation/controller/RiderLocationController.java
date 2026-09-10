package com.chapchap.delivery.domain.riderlocation.controller;

import com.chapchap.delivery.domain.riderlocation.request.RiderLocationUpdateRequest;
import com.chapchap.delivery.domain.riderlocation.response.RiderLocationResponse;
import com.chapchap.delivery.domain.riderlocation.service.RiderLocationService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/rider/location")
public class RiderLocationController {
    private final RiderLocationService locationService;

    @PutMapping
    @PreAuthorize("hasRole('RIDER')")
    public ApiResponse<RiderLocationResponse> update(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody RiderLocationUpdateRequest request
    ) {
        return ApiResponse.success(locationService.update(user.userId(), user.role(), request));
    }
}
