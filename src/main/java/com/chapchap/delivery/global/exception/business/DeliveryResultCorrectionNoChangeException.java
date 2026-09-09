package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryResultCorrectionNoChangeException extends BusinessException {
    public DeliveryResultCorrectionNoChangeException() {
        super(ErrorCode.DELIVERY_RESULT_CORRECTION_NO_CHANGE);
    }
}
