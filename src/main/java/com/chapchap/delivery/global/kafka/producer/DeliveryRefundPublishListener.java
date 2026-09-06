package com.chapchap.delivery.global.kafka.producer;

import com.chapchap.delivery.global.kafka.event.DeliveryRefundConfirmedEvent;
import com.chapchap.delivery.global.kafka.event.DeliveryRefundPublishRequested;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DeliveryRefundPublishListener {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final DeliveryRefundEventProducer producer;
    private final DeliveryEventPublishResultService resultService;

    public DeliveryRefundPublishListener(
        DeliveryRefundEventProducer producer
        , DeliveryEventPublishResultService resultService
    ) {
        this.producer = producer;
        this.resultService = resultService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(DeliveryRefundPublishRequested request) {
        DeliveryRefundConfirmedEvent event = request.event();
        LocalDateTime attemptedAt = LocalDateTime.now(KST);
        try {
            producer.send(event).join();
            resultService.recordRefundSuccess(event, producer.topic(), LocalDateTime.now(KST));
        } catch (RuntimeException exception) {
            resultService.recordRefundFailure(event, producer.topic(), exception, attemptedAt);
        }
    }
}
