package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class DeliveryAssignmentStateConflictException extends BusinessException {
    public DeliveryAssignmentStateConflictException() {
        super(
            ErrorCode.DELIVERY_ASSIGNMENT_STATE_CONFLICT
            , ErrorCode.DELIVERY_ASSIGNMENT_STATE_CONFLICT.getMessage()
        );
    }
}
