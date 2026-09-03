package com.chapchap.delivery.global.kafka.consumer;

import com.chapchap.delivery.domain.access.service.AuthUserEventService;
import com.chapchap.delivery.global.exception.technical.InvalidAuthUserEventException;
import com.chapchap.delivery.global.kafka.event.AuthUserEvent;
import com.chapchap.delivery.global.kafka.validator.AuthUserEventValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.doThrow;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserEventConsumerTest {

    @Mock
    private AuthUserEventValidator validator;

    @Mock
    private AuthUserEventService authUserEventService;

    @InjectMocks
    private AuthUserEventConsumer consumer;

    @Test
    @DisplayName("지원하는 Auth Event는 검증 후 Service에서 처리한다")
    void handleSupportedEventSuccess() {
        AuthUserEvent event =
            createRoleChangedEvent();

        when(
            validator.supports(event)
        ).thenReturn(true);

        consumer.handleAuthUserEvent(
            event
            , "25"
        );

        InOrder inOrder =
            inOrder(
                validator
                , authUserEventService
            );

        inOrder.verify(validator)
            .validate(
                "25"
                , event
            );

        inOrder.verify(validator)
            .supports(event);

        inOrder.verify(authUserEventService)
            .process(event);
    }

    @Test
    @DisplayName("지원하지 않는 Auth Event는 검증 후 정상 무시한다")
    void handleUnsupportedEventIgnore() {
        AuthUserEvent event =
            createUserRegisteredEvent();

        when(
            validator.supports(event)
        ).thenReturn(false);

        consumer.handleAuthUserEvent(
            event
            , "25"
        );

        InOrder inOrder =
            inOrder(
                validator
                , authUserEventService
            );

        inOrder.verify(validator)
            .validate(
                "25"
                , event
            );

        inOrder.verify(validator)
            .supports(event);

        verify(
            authUserEventService
            , never()
        ).process(event);
    }

    @Test
    @DisplayName("Auth Event 검증에 실패하면 Service를 호출하지 않는다")
    void handleInvalidEventFail() {
        AuthUserEvent event =
            createRoleChangedEvent();

        InvalidAuthUserEventException exception =
            new InvalidAuthUserEventException(
                "Invalid auth user event field: messageKey"
            );

        doThrow(exception)
            .when(validator)
            .validate(
                "26"
                , event
            );

        assertThrows(
            InvalidAuthUserEventException.class
            , () -> consumer.handleAuthUserEvent(
                event
                , "26"
            )
        );

        verify(
            validator
            , never()
        ).supports(event);

        verify(
            authUserEventService
            , never()
        ).process(event);
    }

    private AuthUserEvent createRoleChangedEvent() {
        return new AuthUserEvent(
            "0198a8fa-533a-7251-a110-b1675f654358"
            , "USER_ROLE_CHANGED"
            , 1
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
    }

    private AuthUserEvent createUserRegisteredEvent() {
        return new AuthUserEvent(
            "0198a8e8-2acd-7b24-a682-b50c6784515a"
            , "USER_REGISTERED"
            , 1
            , OffsetDateTime.parse(
            "2026-08-16T21:00:00+09:00"
        )
            , 25L
            , new AuthUserEvent.Data(
            "CUSTOMER"
            , null
            , null
            , null
            , null
        )
        );
    }
}