package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.delivery.constant.ContactResult;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryFailureReasonException;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class DeliveryFailureValidator {
    public void validate(
        DeliveryFailureCode failureCode
        , String failureDetail
        , OffsetDateTime contactAttemptedAt
        , ContactResult contactResult
        , Boolean itemRecovered
        , OffsetDateTime recoveredAt
    ) {
        if (failureCode.requiresDetail() && isBlank(failureDetail)) {
            throw new InvalidDeliveryFailureReasonException();
        }

        if (Boolean.TRUE.equals(itemRecovered) != (recoveredAt != null)) {
            throw new InvalidDeliveryFailureReasonException();
        }

        if (failureCode == DeliveryFailureCode.CUSTOMER_UNAVAILABLE) {
            if (
                contactAttemptedAt == null
                    || contactResult == null
                    || !Boolean.TRUE.equals(itemRecovered)
                    || recoveredAt == null
            ) {
                throw new InvalidDeliveryFailureReasonException();
            }
        }

        if (failureCode == DeliveryFailureCode.CUSTOMER_REFUSED
            && (contactAttemptedAt == null || contactResult != ContactResult.CONTACTED)) {
            throw new InvalidDeliveryFailureReasonException();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
