package com.chapchap.delivery.global.exception.technical;

import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.global.exception.TechnicalException;

public class DeliverySlotConfigurationException
    extends TechnicalException {

    public DeliverySlotConfigurationException(
        DeliverySlotCode deliverySlotCode
    ) {
        super(
            "활성 배송 시간대 기준 정보를 찾을 수 없습니다. code="
                + deliverySlotCode
        );
    }
}