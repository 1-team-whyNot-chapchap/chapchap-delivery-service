package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.global.kafka.constant.DeliveryOperationNotificationBusinessKey;
import com.chapchap.delivery.global.kafka.event.DeliveryOperationNotificationData;
import com.chapchap.delivery.global.kafka.event.DeliveryOperationNotificationRequestedEvent;
import com.chapchap.delivery.global.kafka.producer.DeliveryOperationNotificationRequestedEventProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class RiderReassignmentNotificationService {

    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private static final String EVENT_TYPE =
        "DELIVERY_OPERATION_NOTIFICATION_REQUESTED";

    private static final String NOTIFICATION_TYPE =
        "RIDER_REASSIGNED";

    private static final String RECIPIENT_TYPE =
        "RIDER";

    private static final String AGGREGATE_TYPE =
        "DELIVERY_ASSIGNMENT";

    private static final long RESPONSE_MINUTES =
        20L;

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final IntegrationEventRecordRepository integrationEventRecordRepository;
    private final DeliveryOperationNotificationRequestedEventProducer deliveryOperationNotificationProducer;
    private final JsonMapper jsonMapper;
    private final String operationNotificationTopic;

    public RiderReassignmentNotificationService(
        DeliveryAssignmentRepository deliveryAssignmentRepository
        , IntegrationEventRecordRepository integrationEventRecordRepository
        , DeliveryOperationNotificationRequestedEventProducer deliveryOperationNotificationProducer
        , JsonMapper jsonMapper
        , @Value("${app.kafka.topics.operation-notification}") String operationNotificationTopic
    ) {
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.integrationEventRecordRepository = integrationEventRecordRepository;
        this.deliveryOperationNotificationProducer = deliveryOperationNotificationProducer;
        this.jsonMapper = jsonMapper;
        this.operationNotificationTopic = operationNotificationTopic;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(
        Long assignmentId
    ) {
        DeliveryAssignment assignment =
            deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(
                    assignmentId
                )
                .orElse(null);

        if (assignment == null) {
            return;
        }

        if (!assignment.isAssigned()) {
            return;
        }

        if (!Boolean.TRUE.equals(
            assignment.getRider()
                .getIsDeliveryActive()
        )) {
            return;
        }

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderReassigned(
                assignment.getId()
            );

        if (
            integrationEventRecordRepository.existsByBusinessKey(
                businessKey
            )
        ) {
            return;
        }

        Long recipientUserId =
            assignment.getRider()
                .getAuthUserId();

        String eventId =
            UUID.randomUUID()
                .toString();

        String eventKey =
            String.valueOf(
                recipientUserId
            );

        OffsetDateTime occurredAt =
            OffsetDateTime.now(KST);

        OffsetDateTime responseDeadline =
            occurredAt.plusMinutes(
                RESPONSE_MINUTES
            );

        DeliveryOperationNotificationData data =
            new DeliveryOperationNotificationData(
                NOTIFICATION_TYPE
                , RECIPIENT_TYPE
                , recipientUserId
                , AGGREGATE_TYPE
                , String.valueOf(assignment.getId())
                , assignment.getDeliveryGroup()
                .getDeliveryDate()
                , assignment.getDeliveryGroup()
                .getSlot()
                .getCode()
                .name()
                , businessKey
                , null
                , null
                , responseDeadline
            );

        DeliveryOperationNotificationRequestedEvent event =
            new DeliveryOperationNotificationRequestedEvent(
                eventId
                , EVENT_TYPE
                , 1
                , occurredAt
                , null
                , data
            );

        String payloadJson = null;

        try {
            payloadJson =
                jsonMapper.writeValueAsString(
                    event
                );

            deliveryOperationNotificationProducer.send(
                    recipientUserId
                    , event
                )
                .join();
        } catch (Exception exception) {
            savePublishFailure(
                assignment
                , eventId
                , eventKey
                , businessKey
                , payloadJson
                , occurredAt
                , exception
            );

            return;
        }

        LocalDateTime processedAt =
            LocalDateTime.now(KST);

        IntegrationEventRecord successRecord =
            IntegrationEventRecord.publishSuccess(
                eventId
                , EVENT_TYPE
                , AGGREGATE_TYPE
                , String.valueOf(assignment.getId())
                , businessKey
                , occurredAt.toLocalDateTime()
                , processedAt
            );

        integrationEventRecordRepository.save(
            successRecord
        );
    }

    private void savePublishFailure(
        DeliveryAssignment assignment
        , String eventId
        , String eventKey
        , String businessKey
        , String payloadJson
        , OffsetDateTime occurredAt
        , Exception exception
    ) {
        Throwable rootCause =
            findRootCause(
                exception
            );

        IntegrationEventRecord record =
            IntegrationEventRecord.publishFailed(
                eventId
                , EVENT_TYPE
                , AGGREGATE_TYPE
                , String.valueOf(assignment.getId())
                , businessKey
                , operationNotificationTopic
                , eventKey
                , payloadJson
                , occurredAt.toLocalDateTime()
                , LocalDateTime.now(KST)
                , truncate(
                    rootCause.getClass()
                        .getSimpleName()
                    , 100
                )
                , truncate(
                    rootCause.getMessage()
                    , 500
                )
            );

        integrationEventRecordRepository.save(
            record
        );
    }

    private Throwable findRootCause(
        Throwable throwable
    ) {
        Throwable current =
            throwable;

        while (current.getCause() != null) {
            current =
                current.getCause();
        }

        return current;
    }

    private String truncate(
        String value
        , int maxLength
    ) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(
            0
            , maxLength
        );
    }
}
