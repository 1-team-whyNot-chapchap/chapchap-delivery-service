package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class CurrentDeliveryNotFoundException extends BusinessException {
    public CurrentDeliveryNotFoundException() {
        super(ErrorCode.CURRENT_DELIVERY_NOT_FOUND);
    }
}
