package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class AssignmentConditionNotMetException extends BusinessException {
    public AssignmentConditionNotMetException() {
        super(
            ErrorCode.ASSIGNMENT_CONDITION_NOT_MET
            , ErrorCode.ASSIGNMENT_CONDITION_NOT_MET.getMessage()
        );
    }
}