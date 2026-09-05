package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryGroupStateConflictException extends BusinessException {
    public DeliveryGroupStateConflictException() {
        super(
            ErrorCode.DELIVERY_GROUP_STATE_CONFLICT
            , ErrorCode.DELIVERY_GROUP_STATE_CONFLICT.getMessage()
        );
    }
}
