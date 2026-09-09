package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryNotFoundException extends BusinessException {
    public DeliveryNotFoundException() {
        super(ErrorCode.DELIVERY_NOT_FOUND);
    }
}
