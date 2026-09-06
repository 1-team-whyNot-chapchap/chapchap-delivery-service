package com.chapchap.delivery.domain.delivery.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminDeliveryGroupListResponse(
    List<AdminDeliveryGroupListItemResponse> items
    , int page
    , int size
    , long totalElements
    , int totalPages
    , boolean hasNext
) {
    public static AdminDeliveryGroupListResponse from(
        Page<AdminDeliveryGroupListItemResponse> page
    ) {
        return new AdminDeliveryGroupListResponse(
            page.getContent()
            , page.getNumber()
            , page.getSize()
            , page.getTotalElements()
            , page.getTotalPages()
            , page.hasNext()
        );
    }
}
