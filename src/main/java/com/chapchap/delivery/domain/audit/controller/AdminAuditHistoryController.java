package com.chapchap.delivery.domain.audit.controller;

import com.chapchap.delivery.domain.audit.response.AdminAuditHistoryListResponse;
import com.chapchap.delivery.domain.audit.service.AdminAuditHistoryQueryService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import com.chapchap.delivery.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery/admin/audit-histories")
@Tag(name = "Admin Audit History", description = "도시락 배송 운영 과정에서 발생한 관리자 작업 이력을 조회합니다.")
public class AdminAuditHistoryController {
    private final AdminAuditHistoryQueryService queryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Audit Histories", description = "대상 유형과 대상 ID 조건으로 관리자 작업 이력을 조회합니다.")
    @ApiErrorCodes
    public ApiResponse<AdminAuditHistoryListResponse> getAuditHistories(
        @AuthenticationPrincipal AuthenticatedUser user
        , @RequestParam(required = false) String entityType
        , @RequestParam(required = false) Long entityId
        , Pageable pageable
    ) {
        return ApiResponse.success(
            queryService.getAuditHistories(
                user.userId(), user.role(), entityType, entityId, pageable
            )
        );
    }
}
