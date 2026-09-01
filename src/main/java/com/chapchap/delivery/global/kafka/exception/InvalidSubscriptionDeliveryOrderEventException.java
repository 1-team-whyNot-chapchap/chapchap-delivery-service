package com.chapchap.delivery.global.kafka.exception;

public class InvalidSubscriptionDeliveryOrderEventException
    extends RuntimeException {

    public InvalidSubscriptionDeliveryOrderEventException(String message) {
        super(message);
    }
}