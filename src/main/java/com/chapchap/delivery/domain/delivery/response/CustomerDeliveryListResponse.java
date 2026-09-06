package com.chapchap.delivery.domain.delivery.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record CustomerDeliveryListResponse(
    List<CustomerDeliveryListItemResponse> items
    , int page
    , int size
    , long totalElements
    , int totalPages
    , boolean hasNext
) {
    public static CustomerDeliveryListResponse from(
        Page<CustomerDeliveryListItemResponse> page
    ) {
        return new CustomerDeliveryListResponse(
            page.getContent()
            , page.getNumber()
            , page.getSize()
            , page.getTotalElements()
            , page.getTotalPages()
            , page.hasNext()
        );
    }
}
