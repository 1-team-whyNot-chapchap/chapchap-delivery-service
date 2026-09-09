package com.chapchap.delivery.global.exception.business;

import com.chapchap.delivery.global.exception.BusinessException;
import com.chapchap.delivery.global.exception.ErrorCode;

public class RiderLeaveReviewDeadlinePassedException extends BusinessException {
    public RiderLeaveReviewDeadlinePassedException() { super(ErrorCode.RIDER_LEAVE_REVIEW_DEADLINE_PASSED); }
}
