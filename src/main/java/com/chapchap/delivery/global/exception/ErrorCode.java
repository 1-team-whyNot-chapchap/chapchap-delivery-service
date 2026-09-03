package com.chapchap.delivery.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    INVALID_REQUEST(
        HttpStatus.BAD_REQUEST
        , "COMMON_001"
        , "잘못된 요청입니다."
    )
    , INTERNAL_SERVER_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR
        , "COMMON_002"
        , "서버 내부 오류가 발생했습니다."
    )
    , RESOURCE_NOT_FOUND(
        HttpStatus.NOT_FOUND
        , "COMMON_003"
        , "요청한 리소스를 찾을 수 없습니다."
    )
    , AUTHENTICATION_REQUIRED(
        HttpStatus.UNAUTHORIZED
        , "COMMON_004"
        , "인증이 필요합니다."
    )

    // Delivery
    , DELIVERY_NOT_FOUND(
        HttpStatus.NOT_FOUND
        , "DELIVERY_001"
        , "배송 대상을 찾을 수 없습니다."
    )
    , DELIVERY_FORBIDDEN(
        HttpStatus.FORBIDDEN
        , "DELIVERY_002"
        , "배송 권한이 없습니다."
    )
    , INVALID_DELIVERY_INFO(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_003"
        , "필수 배송 정보가 올바르지 않습니다."
    )
    , DELIVERY_STATE_CONFLICT(
        HttpStatus.CONFLICT
        , "DELIVERY_004"
        , "배송 상태가 요청과 충돌합니다."
    )
    , ASSIGNMENT_CONDITION_NOT_MET(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_005"
        , "배정 조건을 충족하지 않습니다."
    )
    , DELIVERY_CAPACITY_EXCEEDED(
        HttpStatus.CONFLICT
        , "DELIVERY_006"
        , "수용량을 초과했습니다."
    )
    , DELIVERY_ALREADY_PROCESSED(
        HttpStatus.CONFLICT
        , "DELIVERY_007"
        , "이미 처리된 배송 요청입니다."
    )
    , COMPLETION_PHOTO_REQUIRED(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_008"
        , "완료 사진이 필요합니다."
    )
    , DELIVERY_RESULT_NOT_CORRECTABLE(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_009"
        , "정정할 수 없는 항목입니다."
    )
    , UNRESOLVED_ASSIGNMENT_ISSUE(
        HttpStatus.CONFLICT
        , "DELIVERY_010"
        , "미해결 기사 이슈가 있습니다."
    )
    , ASSIGNMENT_CHANGE_NOT_ALLOWED(
        HttpStatus.CONFLICT
        , "DELIVERY_011"
        , "최종 확정 이후에는 배정을 변경할 수 없습니다."
    )
    , INVALID_ASSIGNMENT_ISSUE_REASON(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_012"
        , "기사 이슈 사유가 올바르지 않습니다."
    )
    , INVALID_DELIVERY_FAILURE_REASON(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_013"
        , "배송 실패 사유가 올바르지 않습니다."
    )
    , OPTIMISTIC_LOCK_CONFLICT(
        HttpStatus.CONFLICT
        , "DELIVERY_014"
        , "낙관적 락 버전이 일치하지 않습니다."
    )
    , KAFKA_EVENT_PUBLISH_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR
        , "DELIVERY_015"
        , "Kafka 이벤트 발행에 실패했습니다."
    )
    , RIDER_NOT_FOUND(
        HttpStatus.NOT_FOUND
        , "DELIVERY_016"
        , "기사 정보를 찾을 수 없습니다."
    )
    , OTHER_REASON_DETAIL_REQUIRED(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_017"
        , "기타 사유를 선택하면 상세 설명이 필요합니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}