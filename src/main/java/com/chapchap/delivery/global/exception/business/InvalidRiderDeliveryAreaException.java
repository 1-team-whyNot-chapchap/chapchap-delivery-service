package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class InvalidRiderDeliveryAreaException extends BusinessException {
    public InvalidRiderDeliveryAreaException() {
        super(ErrorCode.INVALID_DELIVERY_INFO);
    }
}