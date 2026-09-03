package com.chapchap.delivery.global.kafka.consumer;

import com.chapchap.delivery.domain.access.service.AuthUserEventService;
import com.chapchap.delivery.global.kafka.event.AuthUserEvent;
import com.chapchap.delivery.global.kafka.validator.AuthUserEventValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUserEventConsumer {
    private final AuthUserEventValidator validator;
    private final AuthUserEventService authUserEventService;

    @KafkaListener(
        topics = "${kafka.topic.auth-user-events}"
        , groupId = "${kafka.consumer-group.auth-user-events}"
        , properties = "spring.json.value.default.type=com.chapchap.delivery.global.kafka.event.AuthUserEvent"
    )
    public void handleAuthUserEvent(
        AuthUserEvent event
        , @Header(KafkaHeaders.RECEIVED_KEY) String messageKey
    ) {
        validator.validate(messageKey, event);

        if (!validator.supports(event)) {
            return;
        }

        authUserEventService.process(event);
    }
}