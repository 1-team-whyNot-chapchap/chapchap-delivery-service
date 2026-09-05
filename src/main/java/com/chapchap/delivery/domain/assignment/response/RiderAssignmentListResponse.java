package com.chapchap.delivery.domain.assignment.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record RiderAssignmentListResponse(
    List<RiderAssignmentListItemResponse> items
    , int page
    , int size
    , long totalElements
    , int totalPages
    , boolean hasNext
) {
    public static RiderAssignmentListResponse from(
        Page<RiderAssignmentListItemResponse> page
    ) {
        return new RiderAssignmentListResponse(
            page.getContent()
            , page.getNumber()
            , page.getSize()
            , page.getTotalElements()
            , page.getTotalPages()
            , page.hasNext()
        );
    }
}