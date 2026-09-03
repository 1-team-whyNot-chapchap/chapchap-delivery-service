package com.chapchap.delivery.global.kafka.validator;

import com.chapchap.delivery.global.exception.technical.InvalidAuthUserEventException;
import com.chapchap.delivery.global.kafka.event.AuthUserEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class AuthUserEventValidator {
    private static final int SUPPORTED_VERSION = 1;

    private static final String USER_ROLE_CHANGED =
        "USER_ROLE_CHANGED";

    private static final String USER_WITHDRAWN =
        "USER_WITHDRAWN";

    private static final String ADMIN_ACCOUNT_DISABLED =
        "ADMIN_ACCOUNT_DISABLED";

    private static final Set<String> SUPPORTED_EVENT_TYPES =
        Set.of(
            USER_ROLE_CHANGED
            , USER_WITHDRAWN
            , ADMIN_ACCOUNT_DISABLED
        );

    private static final Set<String> ROLE_CHANGE_ROLES =
        Set.of(
            "CUSTOMER"
            , "RIDER"
        );

    public boolean supports(AuthUserEvent event) {
        return event != null
            && SUPPORTED_EVENT_TYPES.contains(event.eventType());
    }

    public void validate(
        String messageKey
        , AuthUserEvent event
    ) {
        validateEnvelope(messageKey, event);

        if (!supports(event)) {
            return;
        }

        validatePayload(event);
    }

    private void validateEnvelope(
        String messageKey
        , AuthUserEvent event
    ) {
        if (event == null) {
            throw invalid("event");
        }

        requireNotBlank(event.eventType(), "eventType");

        if (event.version() == null
            || event.version() != SUPPORTED_VERSION) {
            throw invalid("version");
        }

        requireNotBlank(event.eventId(), "eventId");
        validateEventId(event.eventId());

        if (event.occurredAt() == null) {
            throw invalid("occurredAt");
        }

        if (event.userId() == null
            || event.userId() <= 0) {
            throw invalid("userId");
        }

        if (event.data() == null) {
            throw invalid("data");
        }

        validateMessageKey(
            messageKey
            , event.userId()
        );
    }

    private void validatePayload(AuthUserEvent event) {
        switch (event.eventType()) {
            case USER_ROLE_CHANGED ->
                validateUserRoleChanged(event.data());

            case USER_WITHDRAWN ->
                validateUserWithdrawn(event.data());

            case ADMIN_ACCOUNT_DISABLED ->
                validateAdminAccountDisabled(event.data());

            default -> {
            }
        }
    }

    private void validateUserRoleChanged(
        AuthUserEvent.Data data
    ) {
        if (!ROLE_CHANGE_ROLES.contains(data.previousRole())) {
            throw invalid("data.previousRole");
        }

        if (!ROLE_CHANGE_ROLES.contains(data.newRole())) {
            throw invalid("data.newRole");
        }

        if (Objects.equals(
            data.previousRole()
            , data.newRole()
        )) {
            throw invalid("data.newRole");
        }
    }

    private void validateUserWithdrawn(
        AuthUserEvent.Data data
    ) {
        if (data.withdrawnAt() == null) {
            throw invalid("data.withdrawnAt");
        }
    }

    private void validateAdminAccountDisabled(
        AuthUserEvent.Data data
    ) {
        if (data.disabledAt() == null) {
            throw invalid("data.disabledAt");
        }
    }

    private void validateMessageKey(
        String messageKey
        , Long userId
    ) {
        if (!Objects.equals(
            String.valueOf(userId)
            , messageKey
        )) {
            throw invalid("messageKey");
        }
    }

    private void requireNotBlank(
        String value
        , String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw invalid(fieldName);
        }
    }

    private void validateEventId(String eventId) {
        try {
            UUID.fromString(eventId);
        } catch (IllegalArgumentException exception) {
            throw invalid("eventId");
        }
    }

    private InvalidAuthUserEventException invalid(
        String fieldName
    ) {
        return new InvalidAuthUserEventException(
            "Invalid auth user event field: "
                + fieldName
        );
    }
}