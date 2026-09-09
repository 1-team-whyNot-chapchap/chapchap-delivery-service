package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRefundReason;
import org.springframework.stereotype.Component;

@Component
public class DeliveryRefundReasonResolver {
    public DeliveryRefundReason resolveFailure(DeliveryFailureCode failureCode) {
        if (
            failureCode == DeliveryFailureCode.WEATHER_CONDITION
                || failureCode == DeliveryFailureCode.ROAD_RESTRICTION
                || failureCode == DeliveryFailureCode.EMERGENCY
        ) {
            return DeliveryRefundReason.FORCE_MAJEURE_CANCELED;
        }

        return DeliveryRefundReason.DELIVERY_FAILED;
    }
}
