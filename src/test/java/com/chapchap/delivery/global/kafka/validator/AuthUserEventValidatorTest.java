package com.chapchap.delivery.global.kafka.validator;

import com.chapchap.delivery.global.exception.technical.InvalidAuthUserEventException;
import com.chapchap.delivery.global.kafka.event.AuthUserEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthUserEventValidatorTest {

    private final AuthUserEventValidator validator =
        new AuthUserEventValidator();

    @Test
    @DisplayName("USER_ROLE_CHANGED 이벤트가 유효하면 검증에 성공한다")
    void validateUserRoleChangedSuccess() {
        AuthUserEvent event =
            createEvent(
                "0198a8fa-533a-7251-a110-b1675f654358"
                , "USER_ROLE_CHANGED"
                , 25L
                , new AuthUserEvent.Data(
                    null
                    , "CUSTOMER"
                    , "RIDER"
                    , null
                    , null
                )
            );

        assertDoesNotThrow(
            () -> validator.validate(
                "25"
                , event
            )
        );
    }

    @Test
    @DisplayName("USER_WITHDRAWN 이벤트가 유효하면 검증에 성공한다")
    void validateUserWithdrawnSuccess() {
        OffsetDateTime withdrawnAt =
            OffsetDateTime.parse(
                "2026-08-16T21:10:00+09:00"
            );

        AuthUserEvent event =
            createEvent(
                "0198a8f1-7652-7f08-9a15-b2d921ff51d3"
                , "USER_WITHDRAWN"
                , 25L
                , new AuthUserEvent.Data(
                    null
                    , null
                    , null
                    , withdrawnAt
                    , null
                )
            );

        assertDoesNotThrow(
            () -> validator.validate(
                "25"
                , event
            )
        );
    }

    @Test
    @DisplayName("ADMIN_ACCOUNT_DISABLED 이벤트가 유효하면 검증에 성공한다")
    void validateAdminAccountDisabledSuccess() {
        OffsetDateTime disabledAt =
            OffsetDateTime.parse(
                "2026-08-16T21:30:00+09:00"
            );

        AuthUserEvent event =
            createEvent(
                "0198a903-6b41-7a2d-b036-49f20670e10b"
                , "ADMIN_ACCOUNT_DISABLED"
                , 9001L
                , new AuthUserEvent.Data(
                    null
                    , null
                    , null
                    , null
                    , disabledAt
                )
            );

        assertDoesNotThrow(
            () -> validator.validate(
                "9001"
                , event
            )
        );
    }

    @Test
    @DisplayName("지원하지 않는 이벤트는 Envelope가 유효하면 정상 통과한다")
    void validateUnsupportedEventSuccess() {
        AuthUserEvent event =
            createEvent(
                "0198a8e8-2acd-7b24-a682-b50c6784515a"
                , "USER_REGISTERED"
                , 25L
                , new AuthUserEvent.Data(
                    "CUSTOMER"
                    , null
                    , null
                    , null
                    , null
                )
            );

        assertDoesNotThrow(
            () -> validator.validate(
                "25"
                , event
            )
        );
    }

    @Test
    @DisplayName("지원하지 않는 이벤트는 supports에서 false를 반환한다")
    void supportsUnsupportedEventFalse() {
        AuthUserEvent event =
            createEvent(
                "0198a8e8-2acd-7b24-a682-b50c6784515a"
                , "USER_REGISTERED"
                , 25L
                , new AuthUserEvent.Data(
                    "CUSTOMER"
                    , null
                    , null
                    , null
                    , null
                )
            );

        org.junit.jupiter.api.Assertions.assertFalse(
            validator.supports(event)
        );
    }

    @Test
    @DisplayName("eventType이 없으면 검증에 실패한다")
    void validateEventTypeNullFail() {
        AuthUserEvent event =
            createEvent(
                "0198a8fa-533a-7251-a110-b1675f654358"
                , null
                , 25L
                , new AuthUserEvent.Data(
                    null
                    , "CUSTOMER"
                    , "RIDER"
                    , null
                    , null
                )
            );

        assertThrows(
            InvalidAuthUserEventException.class
            , () -> validator.validate(
                "25"
                , event
            )
        );
    }

    @Test
    @DisplayName("지원하지 않는 Schema Version이면 검증에 실패한다")
    void validateUnsupportedVersionFail() {
        AuthUserEvent event =
            new AuthUserEvent(
                "0198a8fa-533a-7251-a110-b1675f654358"
                , "USER_ROLE_CHANGED"
                , 2
                , OffsetDateTime.parse(
                "2026-08-16T21:20:00+09:00"
            )
                , 25L
                , new AuthUserEvent.Data(
                null
                , "CUSTOMER"
                , "RIDER"
                , null
                , null
            )
            );

        assertThrows(
            InvalidAuthUserEventException.class
            , () -> validator.validate(
                "25"
                , event
            )
        );
    }

    @Test
    @DisplayName("Message Key와 userId가 다르면 검증에 실패한다")
    void validateMessageKeyMismatchFail() {
        AuthUserEvent event =
            createEvent(
                "0198a8fa-533a-7251-a110-b1675f654358"
                , "USER_ROLE_CHANGED"
                , 25L
                , new AuthUserEvent.Data(
                    null
                    , "CUSTOMER"
                    , "RIDER"
                    , null
                    , null
                )
            );

        assertThrows(
            InvalidAuthUserEventException.class
            , () -> validator.validate(
                "26"
                , event
            )
        );
    }

    @Test
    @DisplayName("USER_ROLE_CHANGED의 이전 역할이 유효하지 않으면 실패한다")
    void validatePreviousRoleInvalidFail() {
        AuthUserEvent event =
            createEvent(
                "0198a8fa-533a-7251-a110-b1675f654358"
                , "USER_ROLE_CHANGED"
                , 25L
                , new AuthUserEvent.Data(
                    null
                    , "ADMIN"
                    , "RIDER"
                    , null
                    , null
                )
            );

        assertThrows(
            InvalidAuthUserEventException.class
            , () -> validator.validate(
                "25"
                , event
            )
        );
    }

    @Test
    @DisplayName("USER_ROLE_CHANGED의 변경 전후 역할이 같으면 실패한다")
    void validateSameRoleFail() {
        AuthUserEvent event =
            createEvent(
                "0198a8fa-533a-7251-a110-b1675f654358"
                , "USER_ROLE_CHANGED"
                , 25L
                , new AuthUserEvent.Data(
                    null
                    , "RIDER"
                    , "RIDER"
                    , null
                    , null
                )
            );

        assertThrows(
            InvalidAuthUserEventException.class
            , () -> validator.validate(
                "25"
                , event
            )
        );
    }

    @Test
    @DisplayName("USER_WITHDRAWN에 withdrawnAt이 없으면 실패한다")
    void validateWithdrawnAtNullFail() {
        AuthUserEvent event =
            createEvent(
                "0198a8f1-7652-7f08-9a15-b2d921ff51d3"
                , "USER_WITHDRAWN"
                , 25L
                , new AuthUserEvent.Data(
                    null
                    , null
                    , null
                    , null
                    , null
                )
            );

        assertThrows(
            InvalidAuthUserEventException.class
            , () -> validator.validate(
                "25"
                , event
            )
        );
    }

    @Test
    @DisplayName("ADMIN_ACCOUNT_DISABLED에 disabledAt이 없으면 실패한다")
    void validateDisabledAtNullFail() {
        AuthUserEvent event =
            createEvent(
                "0198a903-6b41-7a2d-b036-49f20670e10b"
                , "ADMIN_ACCOUNT_DISABLED"
                , 9001L
                , new AuthUserEvent.Data(
                    null
                    , null
                    , null
                    , null
                    , null
                )
            );

        assertThrows(
            InvalidAuthUserEventException.class
            , () -> validator.validate(
                "9001"
                , event
            )
        );
    }

    private AuthUserEvent createEvent(
        String eventId
        , String eventType
        , Long userId
        , AuthUserEvent.Data data
    ) {
        return new AuthUserEvent(
            eventId
            , eventType
            , 1
            , OffsetDateTime.parse(
            "2026-08-16T21:20:00+09:00"
        )
            , userId
            , data
        );
    }
}