package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryAccessForbiddenException extends BusinessException {
    public DeliveryAccessForbiddenException() {
        super(
            ErrorCode.DELIVERY_FORBIDDEN
            , ErrorCode.DELIVERY_FORBIDDEN.getMessage()
        );
    }
}