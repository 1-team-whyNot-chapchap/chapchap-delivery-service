package com.chapchap.delivery.domain.riderlocation.service;

import com.chapchap.delivery.domain.riderlocation.response.RiderLocationResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Single-replica in-memory registry. A delivery can have many browser subscriptions. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiderLocationSseRegistry {
    private final Map<String, Map<String, SseEmitter>> subscriptions = new ConcurrentHashMap<>();
    private final Clock kstClock;

    public SseEmitter subscribe(String deliveryPublicId, long timeoutMillis) throws IOException {
        String subscriptionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        subscriptions.computeIfAbsent(deliveryPublicId, ignored -> new ConcurrentHashMap<>())
            .put(subscriptionId, emitter);
        Runnable cleanup = () -> remove(deliveryPublicId, subscriptionId);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("deliveryId", deliveryPublicId)));
        } catch (IOException exception) {
            cleanup.run();
            throw exception;
        }
        return emitter;
    }

    public void sendLocation(String deliveryPublicId, RiderLocationResponse location) {
        sendToDelivery(deliveryPublicId, "rider-location", location);
    }

    public void sendHeartbeat() {
        subscriptions.keySet().forEach(deliveryId ->
            sendToDelivery(deliveryId, "heartbeat", Map.of("sentAt", OffsetDateTime.now(kstClock)))
        );
    }

    public void endTracking(String deliveryPublicId, String status) {
        Map<String, SseEmitter> emitters = subscriptions.remove(deliveryPublicId);
        if (emitters == null) return;
        emitters.forEach((subscriptionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("tracking-ended").data(Map.of(
                    "deliveryId", deliveryPublicId, "status", status
                )));
            } catch (IOException exception) {
                log.debug("Rider location SSE send failed while ending subscription");
            } finally {
                emitter.complete();
            }
        });
    }

    private void sendToDelivery(String deliveryPublicId, String eventName, Object payload) {
        Map<String, SseEmitter> emitters = subscriptions.get(deliveryPublicId);
        if (emitters == null) return;
        emitters.forEach((subscriptionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException exception) {
                log.debug("Rider location SSE send failed");
                remove(deliveryPublicId, subscriptionId);
                emitter.completeWithError(exception);
            }
        });
    }

    private void remove(String deliveryPublicId, String subscriptionId) {
        subscriptions.computeIfPresent(deliveryPublicId, (ignored, emitters) -> {
            emitters.remove(subscriptionId);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
