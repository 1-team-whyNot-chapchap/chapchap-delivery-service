package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryStateConflictException extends BusinessException {
    public DeliveryStateConflictException() {
        super(ErrorCode.DELIVERY_STATE_CONFLICT);
    }
}
