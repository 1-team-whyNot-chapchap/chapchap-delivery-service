package com.chapchap.delivery.domain.delivery.response;

import com.chapchap.delivery.domain.delivery.constant.DeliveryResultType;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminDeliveryResultCorrectionResponse(
    String deliveryId
    , DeliveryResultType resultType
    , List<Change> changes
    , Long correctedBy
    , OffsetDateTime correctedAt
) {
    public record Change(
        Long correctionId
        , String fieldName
        , String beforeValue
        , String afterValue
    ) {
    }
}
