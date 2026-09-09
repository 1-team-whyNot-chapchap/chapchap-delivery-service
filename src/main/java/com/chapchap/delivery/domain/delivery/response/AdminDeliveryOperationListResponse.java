package com.chapchap.delivery.domain.delivery.response;

import java.util.List;

public record AdminDeliveryOperationListResponse(
    List<AdminDeliveryOperationItemResponse> items
    , int page
    , int size
    , long totalElements
    , int totalPages
    , boolean hasNext
) {
}
