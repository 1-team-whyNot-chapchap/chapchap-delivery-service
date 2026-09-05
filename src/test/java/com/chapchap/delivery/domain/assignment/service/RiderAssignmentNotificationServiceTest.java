package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.global.kafka.event.DeliveryOperationNotificationRequestedEvent;
import com.chapchap.delivery.global.kafka.producer.DeliveryOperationNotificationRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderAssignmentNotificationServiceTest {
    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private IntegrationEventRecordRepository integrationEventRecordRepository;

    @Mock
    private DeliveryOperationNotificationRequestedEventProducer deliveryOperationNotificationProducer;

    @Mock
    private JsonMapper jsonMapper;

    private RiderAssignmentNotificationService riderAssignmentNotificationService;

    @BeforeEach
    void setUp() {
        riderAssignmentNotificationService =
            new RiderAssignmentNotificationService(
                deliveryAssignmentRepository
                , integrationEventRecordRepository
                , deliveryOperationNotificationProducer
                , jsonMapper
                , "delivery.operation-notification-requests.v1"
            );
    }

    @Test
    void publishMarksNotifiedWhenKafkaSendSucceeds() throws Exception {
        Long assignmentId = 1L;
        Long riderAuthUserId = 100L;

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        Rider rider =
            mock(Rider.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        DeliverySlot slot =
            mock(DeliverySlot.class);

        when(assignment.getId())
            .thenReturn(assignmentId);

        when(assignment.getRider())
            .thenReturn(rider);

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(rider.getAuthUserId())
            .thenReturn(riderAuthUserId);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(
                LocalDate.of(
                    2026
                    , 9
                    , 5
                )
            );

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(slot.getCode())
            .thenReturn(DeliverySlotCode.LUNCH);

        when(
            deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(
                assignmentId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(
            jsonMapper.writeValueAsString(
                any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenReturn(
                "{\"eventType\":\"DELIVERY_OPERATION_NOTIFICATION_REQUESTED\"}"
            );

        CompletableFuture<SendResult<String, Object>> successFuture =
            CompletableFuture.completedFuture(
                null
            );

        when(
            deliveryOperationNotificationProducer.send(
                eq(riderAuthUserId)
                , any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenReturn(successFuture);

        riderAssignmentNotificationService.publish(
            assignmentId
        );

        verify(assignment)
            .markNotified(
                any(LocalDateTime.class)
            );

        verify(integrationEventRecordRepository, never())
            .save(
                any(IntegrationEventRecord.class)
            );
    }

    @Test
    void publishSavesFailureRecordWhenKafkaSendFails() throws Exception {
        Long assignmentId = 1L;
        Long riderAuthUserId = 100L;

        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        Rider rider =
            mock(Rider.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        DeliverySlot slot =
            mock(DeliverySlot.class);

        when(assignment.getId())
            .thenReturn(assignmentId);

        when(assignment.getRider())
            .thenReturn(rider);

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(rider.getAuthUserId())
            .thenReturn(riderAuthUserId);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(
                LocalDate.of(
                    2026
                    , 9
                    , 5
                )
            );

        when(deliveryGroup.getSlot())
            .thenReturn(slot);

        when(slot.getCode())
            .thenReturn(DeliverySlotCode.LUNCH);

        when(
            deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(
                assignmentId
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(
            jsonMapper.writeValueAsString(
                any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenReturn(
                "{\"eventType\":\"DELIVERY_OPERATION_NOTIFICATION_REQUESTED\"}"
            );

        CompletableFuture<SendResult<String, Object>> failedFuture =
            new CompletableFuture<>();

        failedFuture.completeExceptionally(
            new RuntimeException(
                "Kafka publish failed"
            )
        );

        when(
            deliveryOperationNotificationProducer.send(
                eq(riderAuthUserId)
                , any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenReturn(failedFuture);

        riderAssignmentNotificationService.publish(
            assignmentId
        );

        verify(assignment, never())
            .markNotified(
                any(LocalDateTime.class)
            );

        ArgumentCaptor<IntegrationEventRecord> recordCaptor =
            ArgumentCaptor.forClass(
                IntegrationEventRecord.class
            );

        verify(integrationEventRecordRepository)
            .save(
                recordCaptor.capture()
            );

        IntegrationEventRecord record =
            recordCaptor.getValue();

        assertThat(record.getDirection().name())
            .isEqualTo("PUBLISH");

        assertThat(record.getStatus().name())
            .isEqualTo("FAILED");

        assertThat(record.getEventType())
            .isEqualTo(
                "DELIVERY_OPERATION_NOTIFICATION_REQUESTED"
            );

        assertThat(record.getAggregateType())
            .isEqualTo(
                "DELIVERY_ASSIGNMENT"
            );

        assertThat(record.getAggregateId())
            .isEqualTo(
                String.valueOf(assignmentId)
            );

        assertThat(record.getBusinessKey())
            .isEqualTo(
                "RIDER_ASSIGNMENT_AVAILABLE:"
                    + assignmentId
            );

        assertThat(record.getTopic())
            .isEqualTo(
                "delivery.operation-notification-requests.v1"
            );

        assertThat(record.getEventKey())
            .isEqualTo(
                String.valueOf(riderAuthUserId)
            );

        assertThat(record.getPayloadJson())
            .isEqualTo(
                "{\"eventType\":\"DELIVERY_OPERATION_NOTIFICATION_REQUESTED\"}"
            );

        assertThat(record.getAttemptCount())
            .isEqualTo(1);

        assertThat(record.getLastAttemptedAt())
            .isNotNull();

        assertThat(record.getOccurredAt())
            .isNotNull();

        assertThat(record.getErrorCode())
            .isEqualTo(
                "RuntimeException"
            );

        assertThat(record.getErrorMessage())
            .isEqualTo(
                "Kafka publish failed"
            );
    }
}