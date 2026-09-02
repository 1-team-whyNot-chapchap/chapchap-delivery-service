package com.chapchap.delivery.global.exception.technical;

import com.chapchap.delivery.global.exception.TechnicalException;

public class InvalidSubscriptionDeliveryOrderEventException
    extends TechnicalException {

    public InvalidSubscriptionDeliveryOrderEventException(String message) {
        super(message);
    }

    public InvalidSubscriptionDeliveryOrderEventException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}