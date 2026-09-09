package com.chapchap.delivery.domain.assignment.response;

import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.RequestHandoffType;

public record RiderAssignmentDeliveryItemResponse(
    String deliveryId
    , DeliveryStatus status
    , Integer lunchboxQuantity
    , String menuName
    , String recipientName
    , String recipientPhone
    , String postalCode
    , String addressLine1
    , String addressLine2
    , String entranceInformation
    , RequestHandoffType requestedHandoffType
    , String otherRequest
    , Boolean termsAgreed
) {
}