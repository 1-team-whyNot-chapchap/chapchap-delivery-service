package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRefundReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DeliveryRefundReasonResolverTest {
    private final DeliveryRefundReasonResolver resolver =
        new DeliveryRefundReasonResolver();

    @ParameterizedTest
    @EnumSource(
        value = DeliveryFailureCode.class
        , names = {
            "WEATHER_CONDITION"
            , "ROAD_RESTRICTION"
            , "EMERGENCY"
        }
    )
    @DisplayName("불가항력 실패는 불가항력 취소 환불 사유로 변환한다")
    void resolvesForceMajeureFailure(DeliveryFailureCode failureCode) {
        assertThat(resolver.resolveFailure(failureCode))
            .isEqualTo(DeliveryRefundReason.FORCE_MAJEURE_CANCELED);
    }

    @ParameterizedTest
    @EnumSource(
        value = DeliveryFailureCode.class
        , mode = EnumSource.Mode.EXCLUDE
        , names = {
            "WEATHER_CONDITION"
            , "ROAD_RESTRICTION"
            , "EMERGENCY"
        }
    )
    @DisplayName("일반 실패는 배송 실패 환불 사유로 변환한다")
    void resolvesGeneralFailure(DeliveryFailureCode failureCode) {
        assertThat(resolver.resolveFailure(failureCode))
            .isEqualTo(DeliveryRefundReason.DELIVERY_FAILED);
    }
}
