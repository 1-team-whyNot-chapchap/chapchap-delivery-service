package com.chapchap.delivery.global.kafka.consumer;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.delivery.service.DeliveryRegistrationService;
import com.chapchap.delivery.domain.delivery.service.IntegrationEventIgnoreService;
import com.chapchap.delivery.global.kafka.event.SubscriptionDeliveryOrderReadyEvent;
import com.chapchap.delivery.global.kafka.validator.SubscriptionDeliveryOrderReadyEventValidator;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionDeliveryOrderReadyEventConsumerTest {
    @Mock private SubscriptionDeliveryOrderReadyEventValidator validator;
    @Mock private DeliveryRegistrationService registrationService;
    @Mock private IntegrationEventIgnoreService ignoreService;
    @InjectMocks private SubscriptionDeliveryOrderReadyEventConsumer consumer;

    @Test
    @DisplayName("지원하지 않는 Subscription Event는 검증 후 정상 무시 기록을 Service에 위임한다")
    void ignoresUnsupportedSubscriptionEvent() {
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-09-07T12:00:00+09:00");
        SubscriptionDeliveryOrderReadyEvent.Data data =
            new SubscriptionDeliveryOrderReadyEvent.Data(
                "order-1", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null
            );
        SubscriptionDeliveryOrderReadyEvent event =
            new SubscriptionDeliveryOrderReadyEvent(
                "0198a8e8-2acd-7b24-a682-b50c6784515c"
                , "UNKNOWN_SUBSCRIPTION_EVENT"
                , 1
                , occurredAt
                , 25L
                , data
            );
        when(validator.supports(event)).thenReturn(false);

        consumer.handleSubscriptionDeliveryOrderReady(event, "order-1");

        var order = inOrder(validator, ignoreService);
        order.verify(validator).validate("order-1", event);
        order.verify(validator).supports(event);
        order.verify(ignoreService).ignore(
            event.eventId(), event.eventType(), "ORDER", "order-1", occurredAt
        );
        verify(registrationService, never()).register(event);
    }
}
