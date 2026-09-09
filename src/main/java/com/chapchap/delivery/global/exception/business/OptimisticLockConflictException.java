package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class OptimisticLockConflictException extends BusinessException {
    public OptimisticLockConflictException() {
        super(
            ErrorCode.OPTIMISTIC_LOCK_CONFLICT
            , ErrorCode.OPTIMISTIC_LOCK_CONFLICT.getMessage()
        );
    }
}