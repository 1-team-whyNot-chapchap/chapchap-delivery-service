package com.chapchap.delivery.global.kafka.consumer;

import com.chapchap.delivery.global.kafka.event.AuthUserEvent;
import com.chapchap.delivery.global.kafka.event.SubscriptionDeliveryOrderReadyEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaListenerDeserializationConfigTest {

    @Test
    @DisplayName("Auth Consumer는 AuthUserEvent 타입으로 역직렬화하도록 설정한다")
    void authUserEventConsumerDeserializationType() throws Exception {
        Method method =
            AuthUserEventConsumer.class.getDeclaredMethod(
                "handleAuthUserEvent"
                , AuthUserEvent.class
                , String.class
            );

        KafkaListener kafkaListener =
            method.getAnnotation(KafkaListener.class);

        assertThat(kafkaListener).isNotNull();
        assertThat(kafkaListener.properties())
            .containsExactly(
                "spring.json.value.default.type=com.chapchap.delivery.global.kafka.event.AuthUserEvent"
            );
    }

    @Test
    @DisplayName("주문 Consumer는 SubscriptionDeliveryOrderReadyEvent 타입으로 역직렬화하도록 설정한다")
    void subscriptionDeliveryOrderConsumerDeserializationType() throws Exception {
        Method method =
            SubscriptionDeliveryOrderReadyEventConsumer.class.getDeclaredMethod(
                "handleSubscriptionDeliveryOrderReady"
                , SubscriptionDeliveryOrderReadyEvent.class
                , String.class
            );

        KafkaListener kafkaListener =
            method.getAnnotation(KafkaListener.class);

        assertThat(kafkaListener).isNotNull();
        assertThat(kafkaListener.properties())
            .containsExactly(
                "spring.json.value.default.type=com.chapchap.delivery.global.kafka.event.SubscriptionDeliveryOrderReadyEvent"
            );
    }
}