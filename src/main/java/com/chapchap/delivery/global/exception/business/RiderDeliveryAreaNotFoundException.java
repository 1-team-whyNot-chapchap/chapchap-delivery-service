package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderDeliveryAreaNotFoundException extends BusinessException {
    public RiderDeliveryAreaNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }
}