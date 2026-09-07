package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class CurrentDeliveryDataConflictException extends BusinessException {
    public CurrentDeliveryDataConflictException() {
        super(ErrorCode.CURRENT_DELIVERY_DATA_CONFLICT);
    }
}
