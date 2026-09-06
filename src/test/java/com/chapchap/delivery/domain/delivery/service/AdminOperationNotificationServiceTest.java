package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
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
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationNotificationServiceTest {
    @Mock private IntegrationEventRecordRepository repository;
    @Mock private DeliveryOperationNotificationRequestedEventProducer producer;
    @Mock private JsonMapper jsonMapper;

    private AdminOperationNotificationService service;

    @BeforeEach
    void setUp() {
        service = new AdminOperationNotificationService(
            repository, producer, jsonMapper, "delivery.operation-notification-requests.v1"
        );
    }

    @Test
    @DisplayName("관리자 운영 알림 성공 시 SUCCESS Event 기록을 저장한다")
    void publishSuccessSavesIntegrationRecord() throws Exception {
        when(repository.existsByBusinessKey("business-1")).thenReturn(false);
        when(jsonMapper.writeValueAsString(any(DeliveryOperationNotificationRequestedEvent.class)))
            .thenReturn("{\"eventType\":\"DELIVERY_OPERATION_NOTIFICATION_REQUESTED\"}");
        when(producer.sendToAdmin(any(DeliveryOperationNotificationRequestedEvent.class)))
            .thenReturn(CompletableFuture.<SendResult<String, Object>>completedFuture(null));

        service.publish(
            "ADMIN_UNRESOLVED_DELIVERY", "DELIVERY_TARGET", "delivery-1",
            LocalDate.of(2026, 9, 7), "LUNCH", "business-1", null
        );

        ArgumentCaptor<IntegrationEventRecord> captor =
            ArgumentCaptor.forClass(IntegrationEventRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(IntegrationEventStatus.SUCCESS);
        assertThat(captor.getValue().getBusinessKey()).isEqualTo("business-1");
        assertThat(captor.getValue().getTopic())
            .isEqualTo("delivery.operation-notification-requests.v1");
        assertThat(captor.getValue().getEventKey()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("관리자 운영 알림 실패 시 FAILED Event 기록을 저장한다")
    void publishFailureSavesIntegrationRecord() throws Exception {
        when(repository.existsByBusinessKey("business-1")).thenReturn(false);
        when(jsonMapper.writeValueAsString(any(DeliveryOperationNotificationRequestedEvent.class)))
            .thenReturn("{\"eventType\":\"DELIVERY_OPERATION_NOTIFICATION_REQUESTED\"}");
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(producer.sendToAdmin(any(DeliveryOperationNotificationRequestedEvent.class)))
            .thenReturn(failed);

        service.publish(
            "ADMIN_UNRESOLVED_DELIVERY", "DELIVERY_TARGET", "delivery-1",
            LocalDate.of(2026, 9, 7), "LUNCH", "business-1", null
        );

        ArgumentCaptor<IntegrationEventRecord> captor =
            ArgumentCaptor.forClass(IntegrationEventRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(IntegrationEventStatus.FAILED);
        assertThat(captor.getValue().getErrorCode()).isEqualTo("KAFKA_EVENT_PUBLISH_FAILED");
    }

    @Test
    @DisplayName("동일 businessKey가 이미 있으면 운영 알림을 중복 발행하지 않는다")
    void duplicateBusinessKeyDoesNotPublish() {
        when(repository.existsByBusinessKey("business-1")).thenReturn(true);

        service.publish(
            "ADMIN_UNRESOLVED_DELIVERY", "DELIVERY_TARGET", "delivery-1",
            LocalDate.of(2026, 9, 7), "LUNCH", "business-1", null
        );

        verify(producer, never()).sendToAdmin(any());
        verify(repository, never()).save(any());
    }
}
