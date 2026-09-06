package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.global.exception.business.InvalidDeliveryFailureReasonException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliveryFailureValidatorTest {
    private static final OffsetDateTime CONTACTED_AT =
        OffsetDateTime.parse("2026-09-06T12:20:00+09:00");
    private static final OffsetDateTime RECOVERED_AT =
        OffsetDateTime.parse("2026-09-06T12:25:00+09:00");

    private final DeliveryFailureValidator validator =
        new DeliveryFailureValidator();

    @Test
    @DisplayName("고객 부재 실패는 연락과 물품 회수 근거가 모두 있으면 허용한다")
    void acceptsCustomerUnavailableWithRequiredEvidence() {
        assertThatCode(
            () -> validator.validate(
                DeliveryFailureCode.CUSTOMER_UNAVAILABLE
                , null
                , CONTACTED_AT
                , "NO_ANSWER"
                , true
                , RECOVERED_AT
            )
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("고객 부재 실패에 연락 시각이 없으면 거절한다")
    void rejectsCustomerUnavailableWithoutContactTime() {
        assertThatThrownBy(
            () -> validator.validate(
                DeliveryFailureCode.CUSTOMER_UNAVAILABLE
                , null
                , null
                , "NO_ANSWER"
                , true
                , RECOVERED_AT
            )
        ).isInstanceOf(InvalidDeliveryFailureReasonException.class);
    }

    @Test
    @DisplayName("고객 부재 실패에 회수 근거가 없으면 거절한다")
    void rejectsCustomerUnavailableWithoutRecoveryEvidence() {
        assertThatThrownBy(
            () -> validator.validate(
                DeliveryFailureCode.CUSTOMER_UNAVAILABLE
                , null
                , CONTACTED_AT
                , "NO_ANSWER"
                , false
                , null
            )
        ).isInstanceOf(InvalidDeliveryFailureReasonException.class);
    }

    @Test
    @DisplayName("OTHER 실패는 상세 설명이 없으면 거절한다")
    void rejectsOtherWithoutDetail() {
        assertThatThrownBy(
            () -> validator.validate(
                DeliveryFailureCode.OTHER
                , " "
                , null
                , null
                , false
                , null
            )
        ).isInstanceOf(InvalidDeliveryFailureReasonException.class);
    }

    @Test
    @DisplayName("회수 여부와 회수 시각이 일치하지 않으면 거절한다")
    void rejectsInconsistentRecoveryEvidence() {
        assertThatThrownBy(
            () -> validator.validate(
                DeliveryFailureCode.ITEM_DAMAGED
                , null
                , null
                , null
                , true
                , null
            )
        ).isInstanceOf(InvalidDeliveryFailureReasonException.class);
    }
}
