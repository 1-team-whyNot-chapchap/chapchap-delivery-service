package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class InvalidDeliveryInfoException extends BusinessException {
    public InvalidDeliveryInfoException() {
        super(ErrorCode.INVALID_DELIVERY_INFO);
    }
}
