package com.chapchap.delivery.domain.audit.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminAuditHistoryListResponse(
    List<AdminAuditHistoryItemResponse> items
    , int page
    , int size
    , long totalElements
    , int totalPages
    , boolean hasNext
) {
    public static AdminAuditHistoryListResponse from(
        Page<AdminAuditHistoryItemResponse> page
    ) {
        return new AdminAuditHistoryListResponse(
            page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
            page.getTotalPages(), page.hasNext()
        );
    }
}
