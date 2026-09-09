package com.chapchap.delivery.global.kafka.event;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record SubscriptionDeliveryOrderReadyEvent(
    String eventId
    , String eventType
    , Integer version
    , OffsetDateTime occurredAt
    , Long userId
    , Data data
) {

    public record Data(
        String orderId
        , LocalDate deliveryDate
        , String deliverySlot
        , Integer lunchboxQuantity
        , String recipientName
        , String recipientPhone
        , String postalCode
        , String addressLine1
        , String addressLine2
        , String deliveryMethod
        , String deliveryMethodDetail
        , String entranceInformation
        , Boolean termsAgreed
        , OffsetDateTime termsAgreedAt
        , List<MenuItem> menuItems
    ) {
    }

    public record MenuItem(
        String menuId
        , String menuName
        , Integer quantity
    ) {
    }
}