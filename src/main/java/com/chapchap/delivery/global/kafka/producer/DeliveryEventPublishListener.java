package com.chapchap.delivery.global.kafka.producer;

import com.chapchap.delivery.global.kafka.event.DeliveryEvent;
import com.chapchap.delivery.global.kafka.event.DeliveryEventPublishRequested;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DeliveryEventPublishListener {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryEventProducer eventProducer;
    private final DeliveryEventPublishResultService resultService;

    public DeliveryEventPublishListener(
        DeliveryEventProducer eventProducer
        , DeliveryEventPublishResultService resultService
    ) {
        this.eventProducer = eventProducer;
        this.resultService = resultService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(
        DeliveryEventPublishRequested request
    ) {
        DeliveryEvent event = request.event();
        LocalDateTime attemptedAt = LocalDateTime.now(KST);

        try {
            eventProducer.send(event).join();
            resultService.recordSuccess(
                event, eventProducer.topic(), LocalDateTime.now(KST)
            );
        } catch (RuntimeException exception) {
            resultService.recordFailure(
                event
                , eventProducer.topic()
                , exception
                , attemptedAt
            );
        }
    }
}
