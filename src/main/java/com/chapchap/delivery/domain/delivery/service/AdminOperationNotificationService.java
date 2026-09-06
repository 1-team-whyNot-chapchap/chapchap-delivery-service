package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.global.kafka.event.DeliveryOperationNotificationData;
import com.chapchap.delivery.global.kafka.event.DeliveryOperationNotificationRequestedEvent;
import com.chapchap.delivery.global.kafka.producer.DeliveryOperationNotificationRequestedEventProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class AdminOperationNotificationService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String EVENT_TYPE = "DELIVERY_OPERATION_NOTIFICATION_REQUESTED";
    private static final String EVENT_KEY = "ADMIN";

    private final IntegrationEventRecordRepository repository;
    private final DeliveryOperationNotificationRequestedEventProducer producer;
    private final JsonMapper jsonMapper;
    private final String topic;

    public AdminOperationNotificationService(
        IntegrationEventRecordRepository repository
        , DeliveryOperationNotificationRequestedEventProducer producer
        , JsonMapper jsonMapper
        , @Value("${app.kafka.topics.operation-notification}") String topic
    ) {
        this.repository = repository;
        this.producer = producer;
        this.jsonMapper = jsonMapper;
        this.topic = topic;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(
        String notificationType, String referenceType, String referenceId,
        LocalDate deliveryDate, String deliverySlot, String businessKey, String actionReason
    ) {
        if (repository.existsByBusinessKey(businessKey)) {
            return;
        }
        String eventId = UUID.randomUUID().toString();
        OffsetDateTime occurredAt = OffsetDateTime.now(KST);
        DeliveryOperationNotificationRequestedEvent event =
            new DeliveryOperationNotificationRequestedEvent(
                eventId, EVENT_TYPE, 1, occurredAt, null,
                new DeliveryOperationNotificationData(
                    notificationType, "ADMIN", null, referenceType, referenceId,
                    deliveryDate, deliverySlot, businessKey, null, actionReason, null
                )
            );
        String payload = null;
        try {
            payload = jsonMapper.writeValueAsString(event);
            producer.sendToAdmin(event).join();
            LocalDateTime processedAt = LocalDateTime.now(KST);
            repository.save(IntegrationEventRecord.publishSuccess(
                eventId, EVENT_TYPE, referenceType, referenceId, businessKey,
                topic, EVENT_KEY, payload, occurredAt.toLocalDateTime(), processedAt
            ));
        } catch (Exception exception) {
            Throwable cause = rootCause(exception);
            repository.save(IntegrationEventRecord.publishFailed(
                eventId, EVENT_TYPE, referenceType, referenceId, businessKey,
                topic, EVENT_KEY, payload, occurredAt.toLocalDateTime(),
                LocalDateTime.now(KST), "KAFKA_EVENT_PUBLISH_FAILED",
                truncate(cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage())
            ));
        }
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private String truncate(String value) {
        return value.substring(0, Math.min(value.length(), 500));
    }
}
