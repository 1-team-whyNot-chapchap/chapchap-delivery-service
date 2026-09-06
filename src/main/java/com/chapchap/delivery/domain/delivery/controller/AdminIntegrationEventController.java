package com.chapchap.delivery.domain.delivery.controller;

import com.chapchap.delivery.domain.delivery.constant.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.response.AdminIntegrationEventListResponse;
import com.chapchap.delivery.domain.delivery.response.AdminIntegrationEventRepublishResponse;
import com.chapchap.delivery.domain.delivery.service.AdminIntegrationEventService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/integration-events")
public class AdminIntegrationEventController {
    private final AdminIntegrationEventService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminIntegrationEventListResponse> getEvents(
        @AuthenticationPrincipal AuthenticatedUser user
        , @RequestParam(required = false) IntegrationEventDirection direction
        , @RequestParam(required = false) IntegrationEventStatus status
        , @RequestParam(required = false) String eventType
        , @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from
        , @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
        , Pageable pageable
    ) {
        return ApiResponse.success(service.getEvents(
            user.userId(), user.role(), direction, status, eventType, from, to, pageable
        ));
    }

    @PostMapping("/{integrationEventRecordId}/republish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminIntegrationEventRepublishResponse> republish(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable Long integrationEventRecordId
    ) {
        return ApiResponse.success(service.republish(
            user.userId(), user.role(), integrationEventRecordId
        ));
    }
}
