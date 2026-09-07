package com.chapchap.delivery.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorCodeContractTest {
    @Test
    @DisplayName("서로 다른 오류 원인은 고유한 코드 문자열을 사용한다")
    void errorCodesAreUnique() {
        var codes = Arrays.stream(ErrorCode.values()).map(ErrorCode::getCode).toList();

        assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Delivery 업무 검증과 Kafka 발행 실패는 공통 오류 코드를 재사용하지 않는다")
    void deliveryErrorsUseDeliveryCodes() {
        assertThat(ErrorCode.INVALID_ASSIGNMENT_ISSUE_REASON.getCode())
            .isEqualTo("DELIVERY_030");
        assertThat(ErrorCode.INVALID_DELIVERY_FAILURE_REASON.getCode())
            .isEqualTo("DELIVERY_031");
        assertThat(ErrorCode.KAFKA_EVENT_PUBLISH_FAILED.getCode())
            .isEqualTo("DELIVERY_032");
        assertThat(ErrorCode.DELIVERY_RESULT_CORRECTION_NO_CHANGE.getCode())
            .isEqualTo("DELIVERY_033");
        assertThat(ErrorCode.DELIVERY_GROUP_CONFIRMATION_CONDITION_NOT_MET.getMessage())
            .isEqualTo("최종 확정 조건을 충족하지 못했습니다. 배송 상세를 다시 확인해 주세요.");
        assertThat(ErrorCode.INTERNAL_SERVICE_AUTHENTICATION_FAILED.getCode())
            .isEqualTo("DELIVERY_034");
        assertThat(ErrorCode.CURRENT_DELIVERY_ACCESS_FORBIDDEN.getCode())
            .isEqualTo("DELIVERY_035");
        assertThat(ErrorCode.CURRENT_DELIVERY_NOT_FOUND.getCode())
            .isEqualTo("DELIVERY_036");
        assertThat(ErrorCode.CURRENT_DELIVERY_DATA_CONFLICT.getCode())
            .isEqualTo("DELIVERY_037");
        assertThat(ErrorCode.INVALID_INTERNAL_SUBJECT.getCode())
            .isEqualTo("DELIVERY_038");
    }
}
