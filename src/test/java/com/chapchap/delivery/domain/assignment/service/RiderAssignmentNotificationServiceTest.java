package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.global.kafka.event.DeliveryOperationNotificationRequestedEvent;
import com.chapchap.delivery.global.kafka.producer.DeliveryOperationNotificationRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderAssignmentNotificationServiceTest {
    private static final String TOPIC = "delivery.operation-notification-requests.v1";

    @Mock private DeliveryAssignmentRepository deliveryAssignmentRepository;
    @Mock private IntegrationEventRecordRepository integrationEventRecordRepository;
    @Mock private DeliveryOperationNotificationRequestedEventProducer producer;
    @Mock private JsonMapper jsonMapper;

    private RiderAssignmentNotificationService service;

    @BeforeEach
    void setUp() {
        service = new RiderAssignmentNotificationService(
            deliveryAssignmentRepository, integrationEventRecordRepository,
            producer, jsonMapper, TOPIC
        );
    }

    @Test
    @DisplayName("ASSIGNED 활성 기사 배정 알림 성공 시 SUCCESS 기록과 notifiedAt을 저장한다")
    void publishSavesSuccessRecordAndMarksNotified() throws Exception {
        Fixture fixture = fixture();
        when(deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(1L))
            .thenReturn(Optional.of(fixture.assignment()));
        when(integrationEventRecordRepository.existsByBusinessKey(
            "RIDER_ASSIGNMENT_AVAILABLE:1"
        )).thenReturn(false);
        when(jsonMapper.writeValueAsString(any(DeliveryOperationNotificationRequestedEvent.class)))
            .thenReturn("{\"eventType\":\"DELIVERY_OPERATION_NOTIFICATION_REQUESTED\"}");
        when(producer.send(eq(100L), any(DeliveryOperationNotificationRequestedEvent.class)))
            .thenReturn(CompletableFuture.<SendResult<String, Object>>completedFuture(null));

        service.publish(1L);

        ArgumentCaptor<IntegrationEventRecord> captor =
            ArgumentCaptor.forClass(IntegrationEventRecord.class);
        verify(integrationEventRecordRepository).save(captor.capture());
        IntegrationEventRecord record = captor.getValue();
        assertThat(record.getStatus()).isEqualTo(IntegrationEventStatus.SUCCESS);
        assertThat(record.getBusinessKey()).isEqualTo("RIDER_ASSIGNMENT_AVAILABLE:1");
        assertThat(record.getTopic()).isEqualTo(TOPIC);
        assertThat(record.getEventKey()).isEqualTo("100");
        assertThat(record.getPayloadJson()).contains("DELIVERY_OPERATION_NOTIFICATION_REQUESTED");
        verify(fixture.assignment()).markNotified(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 FAILED 기록을 저장하고 notifiedAt은 변경하지 않는다")
    void publishSavesFailureRecordWhenKafkaSendFails() throws Exception {
        Fixture fixture = fixture();
        when(deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(1L))
            .thenReturn(Optional.of(fixture.assignment()));
        when(integrationEventRecordRepository.existsByBusinessKey(
            "RIDER_ASSIGNMENT_AVAILABLE:1"
        )).thenReturn(false);
        when(jsonMapper.writeValueAsString(any(DeliveryOperationNotificationRequestedEvent.class)))
            .thenReturn("{\"eventType\":\"DELIVERY_OPERATION_NOTIFICATION_REQUESTED\"}");
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Kafka publish failed"));
        when(producer.send(eq(100L), any(DeliveryOperationNotificationRequestedEvent.class)))
            .thenReturn(failed);

        service.publish(1L);

        ArgumentCaptor<IntegrationEventRecord> captor =
            ArgumentCaptor.forClass(IntegrationEventRecord.class);
        verify(integrationEventRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(IntegrationEventStatus.FAILED);
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(1);
        verify(fixture.assignment(), never()).markNotified(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("ASSIGNED가 아닌 stale 배정은 신규 배정 알림을 발행하지 않는다")
    void doesNotPublishWhenAssignmentIsNotAssigned() {
        Fixture fixture = fixture();
        when(fixture.assignment().isAssigned()).thenReturn(false);
        when(deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(1L))
            .thenReturn(Optional.of(fixture.assignment()));

        service.publish(1L);

        verify(producer, never()).send(any(), any());
        verify(integrationEventRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("배송 업무 비활성 기사에게 신규 배정 알림을 발행하지 않는다")
    void doesNotPublishWhenRiderIsInactive() {
        Fixture fixture = fixture();
        when(fixture.rider().getIsDeliveryActive()).thenReturn(false);
        when(deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(1L))
            .thenReturn(Optional.of(fixture.assignment()));

        service.publish(1L);

        verify(producer, never()).send(any(), any());
        verify(integrationEventRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("동일 businessKey 기록이 있으면 중복 신규 배정 알림을 발행하지 않는다")
    void doesNotPublishDuplicateBusinessKey() {
        Fixture fixture = fixture();
        when(deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(1L))
            .thenReturn(Optional.of(fixture.assignment()));
        when(integrationEventRecordRepository.existsByBusinessKey(
            "RIDER_ASSIGNMENT_AVAILABLE:1"
        )).thenReturn(true);

        service.publish(1L);

        verify(producer, never()).send(any(), any());
        verify(integrationEventRecordRepository, never()).save(any());
    }

    private Fixture fixture() {
        DeliveryAssignment assignment = mock(DeliveryAssignment.class);
        Rider rider = mock(Rider.class);
        DeliveryGroup group = mock(DeliveryGroup.class);
        DeliverySlot slot = mock(DeliverySlot.class);

        lenient().when(assignment.getId()).thenReturn(1L);
        lenient().when(assignment.isAssigned()).thenReturn(true);
        lenient().when(assignment.getRider()).thenReturn(rider);
        lenient().when(assignment.getDeliveryGroup()).thenReturn(group);
        lenient().when(rider.getAuthUserId()).thenReturn(100L);
        lenient().when(rider.getIsDeliveryActive()).thenReturn(true);
        lenient().when(group.getDeliveryDate()).thenReturn(LocalDate.of(2026, 9, 7));
        lenient().when(group.getSlot()).thenReturn(slot);
        lenient().when(slot.getCode()).thenReturn(DeliverySlotCode.LUNCH);
        return new Fixture(assignment, rider);
    }

    private record Fixture(DeliveryAssignment assignment, Rider rider) {
    }
}
