package com.chapchap.delivery.domain.rider.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminRiderLeaveRequestListResponse(
    List<RiderLeaveRequestResponse> items, int page, int size, long totalElements, int totalPages, boolean hasNext
) {
    public static AdminRiderLeaveRequestListResponse from(Page<RiderLeaveRequestResponse> page) {
        return new AdminRiderLeaveRequestListResponse(page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.hasNext());
    }
}
