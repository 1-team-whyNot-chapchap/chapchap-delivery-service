package com.chapchap.delivery.global.kafka.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.delivery.constant.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.global.kafka.event.DeliveryEvent;
import com.chapchap.delivery.global.kafka.event.DeliveryEventData;
import com.chapchap.delivery.global.kafka.event.DeliveryRefundConfirmedData;
import com.chapchap.delivery.global.kafka.event.DeliveryRefundConfirmedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryEventPublishResultServiceTest {
    private static final String DELIVERY_ID = "delivery-public-id";
    private static final String TOPIC = "delivery.delivery-events.v1";

    @Mock private IntegrationEventRecordRepository eventRecordRepository;
    @Mock private ObjectMapper objectMapper;

    private DeliveryEventPublishResultService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryEventPublishResultService(eventRecordRepository, objectMapper);
    }

    @Test
    @DisplayName("배송 상태 Event 발행 성공은 공개 deliveryId와 업무키로 기록한다")
    void recordsDeliveryEventSuccess() throws Exception {
        DeliveryEvent event = new DeliveryEvent(
            "event-id"
            , "DELIVERY_COMPLETED"
            , 1
            , OffsetDateTime.parse("2026-09-06T12:00:00+09:00")
            , 100L
            , new DeliveryEventData(DELIVERY_ID, 3)
        );
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"eventId\":\"event-id\"}");

        service.recordSuccess(event, TOPIC, LocalDateTime.of(2026, 9, 6, 12, 0, 1));

        ArgumentCaptor<IntegrationEventRecord> captor =
            ArgumentCaptor.forClass(IntegrationEventRecord.class);
        verify(eventRecordRepository).save(captor.capture());
        IntegrationEventRecord record = captor.getValue();

        assertThat(record.getDirection()).isEqualTo(IntegrationEventDirection.PUBLISH);
        assertThat(record.getStatus()).isEqualTo(IntegrationEventStatus.SUCCESS);
        assertThat(record.getAggregateId()).isEqualTo(DELIVERY_ID);
        assertThat(record.getBusinessKey()).isEqualTo("DELIVERY_COMPLETED:" + DELIVERY_ID);
        assertThat(record.getTopic()).isEqualTo(TOPIC);
        assertThat(record.getEventKey()).isEqualTo(DELIVERY_ID);
        assertThat(record.getPayloadJson()).contains("event-id");
    }

    @Test
    @DisplayName("발행 실패 오류 메시지는 운영 기록 컬럼 길이에 맞춰 500자로 제한한다")
    void truncatesPublishFailureMessage() throws Exception {
        DeliveryEvent event = new DeliveryEvent(
            "event-id"
            , "DELIVERY_FAILED"
            , 1
            , OffsetDateTime.parse("2026-09-06T12:00:00+09:00")
            , 100L
            , new DeliveryEventData(DELIVERY_ID, 4)
        );
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        RuntimeException failure = new RuntimeException("x".repeat(600));

        service.recordFailure(
            event
            , TOPIC
            , failure
            , LocalDateTime.of(2026, 9, 6, 12, 0, 1)
        );

        ArgumentCaptor<IntegrationEventRecord> captor =
            ArgumentCaptor.forClass(IntegrationEventRecord.class);
        verify(eventRecordRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(IntegrationEventStatus.FAILED);
        assertThat(captor.getValue().getErrorCode()).isEqualTo("KAFKA_EVENT_PUBLISH_FAILED");
        assertThat(captor.getValue().getErrorMessage()).hasSize(500);
    }

    @Test
    @DisplayName("환불 확정 Event는 배송 대상당 하나의 환불 업무키로 기록한다")
    void recordsRefundBusinessKeyPerDelivery() throws Exception {
        OffsetDateTime confirmedAt = OffsetDateTime.parse("2026-09-06T12:10:00+09:00");
        DeliveryRefundConfirmedEvent event = new DeliveryRefundConfirmedEvent(
            "refund-event-id"
            , "DELIVERY_REFUND_CONFIRMED"
            , 1
            , confirmedAt
            , 100L
            , new DeliveryRefundConfirmedData(
                DELIVERY_ID
                , "order-public-id"
                , confirmedAt
                , "DELIVERY_FAILED"
            )
        );
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");

        service.recordRefundSuccess(
            event
            , "delivery.refund-events.v1"
            , LocalDateTime.of(2026, 9, 6, 12, 10, 1)
        );

        ArgumentCaptor<IntegrationEventRecord> captor =
            ArgumentCaptor.forClass(IntegrationEventRecord.class);
        verify(eventRecordRepository).save(captor.capture());

        assertThat(captor.getValue().getBusinessKey())
            .isEqualTo("REFUND:DELIVERY:" + DELIVERY_ID);
    }
}
