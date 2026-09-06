package com.chapchap.delivery.domain.delivery.constant;

public enum DeliveryFailureCode {
    CUSTOMER_UNAVAILABLE
    , ACCESS_DENIED
    , INVALID_ADDRESS
    , CUSTOMER_REFUSED

    , RIDER_UNAVAILABLE
    , RIDER_ACCIDENT
    , VEHICLE_ISSUE
    , DELIVERY_OMITTED

    , ITEM_MISSING
    , ITEM_DAMAGED

    , WEATHER_CONDITION
    , ROAD_RESTRICTION
    , EMERGENCY

    , OTHER
    ;

    public boolean requiresDetail() {
        return this == OTHER;
    }
}