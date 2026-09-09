package com.chapchap.delivery.domain.delivery.controller;

import static com.chapchap.delivery.global.exception.ErrorCode.INTEGRATION_EVENT_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.INTEGRATION_EVENT_NOT_REPUBLISHABLE;

import com.chapchap.delivery.domain.delivery.constant.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.response.AdminIntegrationEventListResponse;
import com.chapchap.delivery.domain.delivery.response.AdminIntegrationEventRepublishResponse;
import com.chapchap.delivery.domain.delivery.service.AdminIntegrationEventService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/integration-events")
@Tag(name = "Admin Integration Event", description = "도시락 배송과 관련된 Kafka 연동 이벤트의 처리 현황과 재발행을 관리합니다.")
public class AdminIntegrationEventController {
    private final AdminIntegrationEventService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Integration Events", description = "방향, 처리 상태, 이벤트 유형과 발생 시간 조건으로 배송 연동 이력을 조회합니다.")
    @ApiErrorCodes
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
    @Operation(summary = "Republish Integration Event", description = "발행에 실패한 배송 Kafka 이벤트 한 건을 다시 발행합니다.")
    @ApiErrorCodes({
        INTEGRATION_EVENT_NOT_FOUND,
        INTEGRATION_EVENT_NOT_REPUBLISHABLE
    })
    public ApiResponse<AdminIntegrationEventRepublishResponse> republish(
        @AuthenticationPrincipal AuthenticatedUser user
        , @PathVariable Long integrationEventRecordId
    ) {
        return ApiResponse.success(service.republish(
            user.userId(), user.role(), integrationEventRecordId
        ));
    }
}
