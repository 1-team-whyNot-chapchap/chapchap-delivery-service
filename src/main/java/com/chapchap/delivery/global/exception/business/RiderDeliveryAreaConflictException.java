package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderDeliveryAreaConflictException extends BusinessException {
    public RiderDeliveryAreaConflictException() {
        super(ErrorCode.DELIVERY_STATE_CONFLICT);
    }
}