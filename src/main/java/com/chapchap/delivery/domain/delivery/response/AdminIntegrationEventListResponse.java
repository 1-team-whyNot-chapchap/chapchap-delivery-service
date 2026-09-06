package com.chapchap.delivery.domain.delivery.response;

import org.springframework.data.domain.Page;
import java.util.List;

public record AdminIntegrationEventListResponse(
    List<AdminIntegrationEventItemResponse> items
    , int page
    , int size
    , long totalElements
    , int totalPages
    , boolean hasNext
) {
    public static AdminIntegrationEventListResponse from(
        Page<AdminIntegrationEventItemResponse> page
    ) {
        return new AdminIntegrationEventListResponse(
            page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.hasNext()
        );
    }
}
