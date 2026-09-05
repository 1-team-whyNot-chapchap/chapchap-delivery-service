package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class InvalidAssignmentIssueReasonException extends BusinessException {
    public InvalidAssignmentIssueReasonException() {
        super(
            ErrorCode.INVALID_ASSIGNMENT_ISSUE_REASON
            , ErrorCode.INVALID_ASSIGNMENT_ISSUE_REASON.getMessage()
        );
    }
}