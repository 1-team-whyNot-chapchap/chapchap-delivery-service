package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryResultNotCorrectableException extends BusinessException {
    public DeliveryResultNotCorrectableException() {
        super(
            ErrorCode.DELIVERY_RESULT_NOT_CORRECTABLE
            , ErrorCode.DELIVERY_RESULT_NOT_CORRECTABLE.getMessage()
        );
    }
}
