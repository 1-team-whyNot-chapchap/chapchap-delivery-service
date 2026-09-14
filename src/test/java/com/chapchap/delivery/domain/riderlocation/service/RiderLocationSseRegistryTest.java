package com.chapchap.delivery.domain.riderlocation.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chapchap.delivery.domain.riderlocation.response.RiderLocationResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class RiderLocationSseRegistryTest {
    private final RiderLocationSseRegistry registry = new RiderLocationSseRegistry(
        Clock.fixed(Instant.parse("2026-09-11T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    );

    @Test
    @DisplayName("같은 배송의 여러 subscription은 함께 종료된다")
    void endsEverySubscriptionForTheSameDelivery() throws Exception {
        SseEmitter first = registry.subscribe("delivery-101", 60_000);
        SseEmitter second = registry.subscribe("delivery-101", 60_000);

        registry.endTracking("delivery-101", "DELIVERED");

        assertThatThrownBy(() -> first.send("after-ended")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> second.send("after-ended")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("한 배송 종료는 다른 배송의 SSE 위치 공유를 끊지 않는다")
    void endingOneDeliveryKeepsAnotherDeliverySubscriptionAlive() throws Exception {
        SseEmitter ended = registry.subscribe("delivery-101", 60_000);
        SseEmitter active = registry.subscribe("delivery-102", 60_000);

        registry.endTracking("delivery-101", "FAILED");
        registry.sendLocation("delivery-102", location());
        registry.sendHeartbeat();

        assertThatThrownBy(() -> ended.send("after-ended")).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> active.send("still-active")).doesNotThrowAnyException();
    }

    private RiderLocationResponse location() {
        OffsetDateTime now = OffsetDateTime.of(2026, 9, 11, 12, 0, 0, 0, ZoneOffset.ofHours(9));
        return new RiderLocationResponse(
            new BigDecimal("37.5665000"), new BigDecimal("126.9780000"), new BigDecimal("12.50"), now, now
        );
    }
}
