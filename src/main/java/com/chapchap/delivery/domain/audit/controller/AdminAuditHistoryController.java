package com.chapchap.delivery.domain.audit.controller;

import com.chapchap.delivery.domain.audit.response.AdminAuditHistoryListResponse;
import com.chapchap.delivery.domain.audit.service.AdminAuditHistoryQueryService;
import com.chapchap.delivery.global.response.ApiResponse;
import com.chapchap.delivery.global.security.AuthenticatedUser;
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
public class AdminAuditHistoryController {
    private final AdminAuditHistoryQueryService queryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
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
