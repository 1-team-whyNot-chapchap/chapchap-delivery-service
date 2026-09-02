package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryRegistrationException extends BusinessException {
    public DeliveryRegistrationException(String message) {
        super(ErrorCode.INVALID_DELIVERY_INFO, message);
    }
}