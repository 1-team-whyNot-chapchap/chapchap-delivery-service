package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryGroupConfirmationConditionNotMetException extends BusinessException {
    public DeliveryGroupConfirmationConditionNotMetException() {
        super(
            ErrorCode.DELIVERY_GROUP_CONFIRMATION_CONDITION_NOT_MET
            , ErrorCode.DELIVERY_GROUP_CONFIRMATION_CONDITION_NOT_MET.getMessage()
        );
    }
}
