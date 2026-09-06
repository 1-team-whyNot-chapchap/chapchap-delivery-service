package com.chapchap.delivery.global.kafka.producer;

import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.global.kafka.event.DeliveryEvent;
import com.chapchap.delivery.global.kafka.event.DeliveryRefundConfirmedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryEventPublishResultService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String AGGREGATE_TYPE = "DELIVERY";
    private static final String PUBLISH_ERROR_CODE = "KAFKA_EVENT_PUBLISH_FAILED";

    private final IntegrationEventRecordRepository eventRecordRepository;
    private final ObjectMapper objectMapper;

    public DeliveryEventPublishResultService(
        IntegrationEventRecordRepository eventRecordRepository
        , ObjectMapper objectMapper
    ) {
        this.eventRecordRepository = eventRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
        DeliveryEvent event
        , String topic
        , LocalDateTime processedAt
    ) {
        eventRecordRepository.save(
            IntegrationEventRecord.publishSuccess(
                event.eventId()
                , event.eventType()
                , AGGREGATE_TYPE
                , event.data().deliveryId()
                , businessKey(event)
                , topic
                , event.data().deliveryId()
                , payload(event)
                , event.occurredAt().atZoneSameInstant(KST).toLocalDateTime()
                , processedAt
            )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
        DeliveryEvent event
        , String topic
        , Throwable failure
        , LocalDateTime attemptedAt
    ) {
        eventRecordRepository.save(
            IntegrationEventRecord.publishFailed(
                event.eventId()
                , event.eventType()
                , AGGREGATE_TYPE
                , event.data().deliveryId()
                , businessKey(event)
                , topic
                , event.data().deliveryId()
                , payload(event)
                , event.occurredAt().atZoneSameInstant(KST).toLocalDateTime()
                , attemptedAt
                , PUBLISH_ERROR_CODE
                , safeMessage(failure)
            )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRefundSuccess(
        DeliveryRefundConfirmedEvent event, String topic, LocalDateTime processedAt
    ) {
        eventRecordRepository.save(IntegrationEventRecord.publishSuccess(
            event.eventId(), event.eventType(), AGGREGATE_TYPE,
            event.data().deliveryId(), refundBusinessKey(event.data().deliveryId()),
            topic, event.data().deliveryId(), payload(event),
            event.occurredAt().atZoneSameInstant(KST).toLocalDateTime(), processedAt
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRefundFailure(
        DeliveryRefundConfirmedEvent event, String topic,
        Throwable failure, LocalDateTime attemptedAt
    ) {
        eventRecordRepository.save(IntegrationEventRecord.publishFailed(
            event.eventId(), event.eventType(), AGGREGATE_TYPE,
            event.data().deliveryId(), refundBusinessKey(event.data().deliveryId()),
            topic, event.data().deliveryId(), payload(event),
            event.occurredAt().atZoneSameInstant(KST).toLocalDateTime(), attemptedAt,
            PUBLISH_ERROR_CODE, safeMessage(failure)
        ));
    }

    private String businessKey(DeliveryEvent event) {
        return event.eventType() + ":" + event.data().deliveryId();
    }

    private String payload(DeliveryEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String payload(DeliveryRefundConfirmedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String refundBusinessKey(String deliveryId) {
        return "REFUND:DELIVERY:" + deliveryId;
    }

    private String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 500));
    }
}
