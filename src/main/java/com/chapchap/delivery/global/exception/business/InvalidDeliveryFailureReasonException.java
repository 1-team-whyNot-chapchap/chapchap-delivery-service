package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class InvalidDeliveryFailureReasonException extends BusinessException {
    public InvalidDeliveryFailureReasonException() {
        super(ErrorCode.INVALID_DELIVERY_FAILURE_REASON);
    }
}
