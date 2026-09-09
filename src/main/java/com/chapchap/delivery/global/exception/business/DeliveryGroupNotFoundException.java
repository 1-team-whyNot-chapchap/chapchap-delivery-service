package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryGroupNotFoundException extends BusinessException {
    public DeliveryGroupNotFoundException() {
        super(
            ErrorCode.DELIVERY_GROUP_NOT_FOUND
            , ErrorCode.DELIVERY_GROUP_NOT_FOUND.getMessage()
        );
    }
}
