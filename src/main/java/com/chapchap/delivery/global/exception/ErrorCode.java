package com.chapchap.delivery.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    INVALID_REQUEST(
        HttpStatus.BAD_REQUEST
        , "COMMON_001"
        , "잘못된 요청 값입니다."
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
    , DELIVERY_GROUP_NOT_FOUND(
        HttpStatus.NOT_FOUND
        , "DELIVERY_002"
        , "전체 배송을 찾을 수 없습니다."
    )
    , DELIVERY_ASSIGNMENT_NOT_FOUND(
        HttpStatus.NOT_FOUND
        , "DELIVERY_003"
        , "배정 목록을 찾을 수 없습니다."
    )
    , DELIVERY_FORBIDDEN(
        HttpStatus.FORBIDDEN
        , "DELIVERY_025"
        , "현재 Delivery 접근이 허용되지 않습니다."
    )
    , INVALID_DELIVERY_INFO(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_011"
        , "필수 배송 정보가 올바르지 않습니다."
    )
    , DELIVERY_STATE_CONFLICT(
        HttpStatus.CONFLICT
        , "DELIVERY_006"
        , "현재 배송 상태에서는 요청을 처리할 수 없습니다."
    )
    , DELIVERY_GROUP_STATE_CONFLICT(
        HttpStatus.CONFLICT
        , "DELIVERY_007"
        , "현재 전체 배송 상태에서는 요청을 처리할 수 없습니다."
    )
    , DELIVERY_ASSIGNMENT_STATE_CONFLICT(
        HttpStatus.CONFLICT
        , "DELIVERY_008"
        , "현재 배정 상태에서는 요청을 처리할 수 없습니다."
    )
    , ASSIGNMENT_CONDITION_NOT_MET(
        HttpStatus.CONFLICT
        , "DELIVERY_013"
        , "기사가 해당 날짜와 시간대에 배정 가능하지 않습니다."
    )
    , DELIVERY_CAPACITY_EXCEEDED(
        HttpStatus.CONFLICT
        , "DELIVERY_012"
        , "기사 수용량 최대치를 초과합니다."
    )
    , DELIVERY_GROUP_CONFIRMATION_CONDITION_NOT_MET(
        HttpStatus.CONFLICT
        , "DELIVERY_014"
        , "관리자 최종 확정 조건을 충족하지 못했습니다."
    )
    , DELIVERY_ALREADY_PROCESSED(
        HttpStatus.CONFLICT
        , "DELIVERY_009"
        , "다른 요청이 먼저 처리되었습니다. 최신 상태를 확인해 주세요."
    )
    , COMPLETION_PHOTO_REQUIRED(
        HttpStatus.NOT_FOUND
        , "DELIVERY_015"
        , "완료 사진이 필요합니다."
    )
    , INVALID_DELIVERY_PHOTO_INFO(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_016"
        , "사진 정보가 올바르지 않습니다."
    )
    , DELIVERY_HANDOFF_INFO_REQUIRED(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_020"
        , "전달 방식에 필요한 정보가 누락되었습니다."
    )
    , DELIVERY_RESULT_NOT_CORRECTABLE(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_023"
        , "정정할 수 없는 항목입니다."
    )
    , ASSIGNMENT_CHANGE_NOT_ALLOWED(
        HttpStatus.CONFLICT
        , "DELIVERY_022"
        , "최종 확정 이후에는 배정을 변경할 수 없습니다."
    )
    , INVALID_ASSIGNMENT_ISSUE_REASON(
        HttpStatus.BAD_REQUEST
        , "COMMON_001"
        , "기사 이슈 사유가 올바르지 않습니다."
    )
    , INVALID_DELIVERY_FAILURE_REASON(
        HttpStatus.BAD_REQUEST
        , "COMMON_001"
        , "배송 실패 사유가 올바르지 않습니다."
    )
    , OPTIMISTIC_LOCK_CONFLICT(
        HttpStatus.CONFLICT
        , "DELIVERY_010"
        , "다른 관리자가 먼저 수정했습니다. 최신 정보를 확인해 주세요."
    )
    , KAFKA_EVENT_PUBLISH_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR
        , "COMMON_002"
        , "Kafka 이벤트 발행에 실패했습니다."
    )
    , RIDER_NOT_FOUND(
        HttpStatus.NOT_FOUND
        , "DELIVERY_024"
        , "기사 정보를 찾을 수 없습니다."
    )
    , OTHER_REASON_DETAIL_REQUIRED(
        HttpStatus.BAD_REQUEST
        , "DELIVERY_021"
        , "기타 사유를 선택하면 상세 설명이 필요합니다."
    )
    , INTEGRATION_EVENT_NOT_FOUND(
        HttpStatus.NOT_FOUND
        , "DELIVERY_028"
        , "이벤트 처리 기록을 찾을 수 없습니다."
    )
    , INTEGRATION_EVENT_NOT_REPUBLISHABLE(
        HttpStatus.CONFLICT
        , "DELIVERY_029"
        , "재발행할 수 없는 이벤트 상태입니다."
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
