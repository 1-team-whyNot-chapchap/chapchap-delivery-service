package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.assignment.entity.DeliveryAssignment;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.global.kafka.constant.DeliveryOperationNotificationBusinessKey;
import com.chapchap.delivery.global.kafka.event.DeliveryOperationNotificationRequestedEvent;
import com.chapchap.delivery.global.kafka.producer.DeliveryOperationNotificationRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderReassignmentNotificationServiceTest {

    private static final Long ASSIGNMENT_ID = 1L;
    private static final Long RIDER_AUTH_USER_ID = 10001L;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private IntegrationEventRecordRepository integrationEventRecordRepository;

    @Mock
    private DeliveryOperationNotificationRequestedEventProducer deliveryOperationNotificationProducer;

    @Mock
    private JsonMapper jsonMapper;

    private RiderReassignmentNotificationService riderReassignmentNotificationService;

    @BeforeEach
    void setUp() {
        riderReassignmentNotificationService =
            new RiderReassignmentNotificationService(
                deliveryAssignmentRepository
                , integrationEventRecordRepository
                , deliveryOperationNotificationProducer
                , jsonMapper
                , "delivery-operation-notification"
            );
    }

    @Test
    @DisplayName("재배정된 기사에게 RIDER_REASSIGNED 알림을 발행하고 응답 기한을 발행 시각부터 20분으로 설정한다")
    void publishPublishesRiderReassignedNotification() throws Exception {
        // given
        LocalDate deliveryDate =
            LocalDate.of(
                2026
                , 9
                , 6
            );

        mockPublishableAssignment(
            deliveryDate
            , DeliverySlotCode.LUNCH
        );

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderReassigned(
                ASSIGNMENT_ID
            );

        when(
            integrationEventRecordRepository.existsByBusinessKey(
                businessKey
            )
        )
            .thenReturn(false);

        when(
            jsonMapper.writeValueAsString(
                any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenReturn("{}");

        when(
            deliveryOperationNotificationProducer.send(
                eq(RIDER_AUTH_USER_ID)
                , any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenReturn(
                CompletableFuture.completedFuture(null)
            );

        // when
        riderReassignmentNotificationService.publish(
            ASSIGNMENT_ID
        );

        // then
        ArgumentCaptor<DeliveryOperationNotificationRequestedEvent> eventCaptor =
            ArgumentCaptor.forClass(
                DeliveryOperationNotificationRequestedEvent.class
            );

        verify(deliveryOperationNotificationProducer)
            .send(
                eq(RIDER_AUTH_USER_ID)
                , eventCaptor.capture()
            );

        DeliveryOperationNotificationRequestedEvent event =
            eventCaptor.getValue();

        assertThat(event.eventType())
            .isEqualTo(
                "DELIVERY_OPERATION_NOTIFICATION_REQUESTED"
            );

        assertThat(event.version())
            .isEqualTo(1);

        assertThat(event.data().notificationType())
            .isEqualTo(
                "RIDER_REASSIGNED"
            );

        assertThat(event.data().recipientType())
            .isEqualTo(
                "RIDER"
            );

        assertThat(event.data().recipientUserId())
            .isEqualTo(
                RIDER_AUTH_USER_ID
            );

        assertThat(event.data().businessKey())
            .isEqualTo(
                businessKey
            );

        assertThat(event.data().deliveryDate())
            .isEqualTo(
                deliveryDate
            );

        assertThat(event.data().deliverySlot())
            .isEqualTo(
                "LUNCH"
            );

        assertThat(event.data().responseDeadline())
            .isEqualTo(
                event.occurredAt()
                    .plusMinutes(20L)
            );

        assertThat(event.data().reminderStage())
            .isNull();

        assertThat(event.data().actionReason())
            .isNull();

        verify(jsonMapper)
            .writeValueAsString(
                event
            );

        verify(integrationEventRecordRepository)
            .save(
                any(IntegrationEventRecord.class)
            );
    }

    @Test
    @DisplayName("저녁 재배정 알림에는 DINNER 배송 슬롯을 포함한다")
    void publishUsesDinnerDeliverySlot() throws Exception {
        // given
        LocalDate deliveryDate =
            LocalDate.of(
                2026
                , 9
                , 6
            );

        mockPublishableAssignment(
            deliveryDate
            , DeliverySlotCode.DINNER
        );

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderReassigned(
                ASSIGNMENT_ID
            );

        when(
            integrationEventRecordRepository.existsByBusinessKey(
                businessKey
            )
        )
            .thenReturn(false);

        when(
            jsonMapper.writeValueAsString(
                any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenReturn("{}");

        when(
            deliveryOperationNotificationProducer.send(
                eq(RIDER_AUTH_USER_ID)
                , any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenReturn(
                CompletableFuture.completedFuture(null)
            );

        // when
        riderReassignmentNotificationService.publish(
            ASSIGNMENT_ID
        );

        // then
        ArgumentCaptor<DeliveryOperationNotificationRequestedEvent> eventCaptor =
            ArgumentCaptor.forClass(
                DeliveryOperationNotificationRequestedEvent.class
            );

        verify(deliveryOperationNotificationProducer)
            .send(
                eq(RIDER_AUTH_USER_ID)
                , eventCaptor.capture()
            );

        DeliveryOperationNotificationRequestedEvent event =
            eventCaptor.getValue();

        assertThat(event.data().deliverySlot())
            .isEqualTo(
                "DINNER"
            );

        assertThat(event.data().responseDeadline())
            .isEqualTo(
                event.occurredAt()
                    .plusMinutes(20L)
            );

        verify(integrationEventRecordRepository)
            .save(
                any(IntegrationEventRecord.class)
            );
    }

    @Test
    @DisplayName("동일한 재배정 businessKey가 이미 존재하면 다시 발행하지 않는다")
    void publishDoesNotPublishWhenBusinessKeyAlreadyExists() {
        // given
        mockPublishableAssignmentUntilRider();

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderReassigned(
                ASSIGNMENT_ID
            );

        when(
            integrationEventRecordRepository.existsByBusinessKey(
                businessKey
            )
        )
            .thenReturn(true);

        // when
        riderReassignmentNotificationService.publish(
            ASSIGNMENT_ID
        );

        // then
        verify(
            deliveryOperationNotificationProducer
            , never()
        )
            .send(
                any()
                , any()
            );

        verify(
            jsonMapper
            , never()
        )
            .writeValueAsString(
                any()
            );

        verify(
            integrationEventRecordRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("재배정된 Assignment가 ASSIGNED 상태가 아니면 알림을 발행하지 않는다")
    void publishDoesNotPublishWhenAssignmentIsNotAssigned() {
        // given
        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        when(
            deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(
                ASSIGNMENT_ID
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.isAssigned())
            .thenReturn(false);

        // when
        riderReassignmentNotificationService.publish(
            ASSIGNMENT_ID
        );

        // then
        verify(
            integrationEventRecordRepository
            , never()
        )
            .existsByBusinessKey(
                any()
            );

        verify(
            deliveryOperationNotificationProducer
            , never()
        )
            .send(
                any()
                , any()
            );

        verify(
            integrationEventRecordRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("배송 비활성 기사에게는 재배정 알림을 발행하지 않는다")
    void publishDoesNotPublishWhenRiderIsNotDeliveryActive() {
        // given
        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        Rider rider =
            mock(Rider.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        when(
            deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(
                ASSIGNMENT_ID
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.isAssigned())
            .thenReturn(true);

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(false);

        // when
        riderReassignmentNotificationService.publish(
            ASSIGNMENT_ID
        );

        // then
        verify(
            integrationEventRecordRepository
            , never()
        )
            .existsByBusinessKey(
                any()
            );

        verify(
            deliveryOperationNotificationProducer
            , never()
        )
            .send(
                any()
                , any()
            );

        verify(
            integrationEventRecordRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("재배정 Assignment가 존재하지 않으면 알림을 발행하지 않는다")
    void publishDoesNotPublishWhenAssignmentDoesNotExist() {
        // given
        when(
            deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(
                ASSIGNMENT_ID
            )
        )
            .thenReturn(
                Optional.empty()
            );

        // when
        riderReassignmentNotificationService.publish(
            ASSIGNMENT_ID
        );

        // then
        verify(
            integrationEventRecordRepository
            , never()
        )
            .existsByBusinessKey(
                any()
            );

        verify(
            deliveryOperationNotificationProducer
            , never()
        )
            .send(
                any()
                , any()
            );

        verify(
            integrationEventRecordRepository
            , never()
        )
            .save(
                any()
            );
    }

    @Test
    @DisplayName("재배정 알림 Kafka 발행에 실패하면 예외를 전파하지 않고 실패 Integration Event를 저장한다")
    void publishSavesFailedIntegrationEventWhenKafkaPublishFails() throws Exception {
        // given
        mockPublishableAssignment(
            LocalDate.of(
                2026
                , 9
                , 6
            )
            , DeliverySlotCode.LUNCH
        );

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderReassigned(
                ASSIGNMENT_ID
            );

        when(
            integrationEventRecordRepository.existsByBusinessKey(
                businessKey
            )
        )
            .thenReturn(false);

        when(
            jsonMapper.writeValueAsString(
                any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenReturn(
                "{\"eventType\":\"DELIVERY_OPERATION_NOTIFICATION_REQUESTED\"}"
            );

        when(
            deliveryOperationNotificationProducer.send(
                eq(RIDER_AUTH_USER_ID)
                , any(DeliveryOperationNotificationRequestedEvent.class)
            )
        )
            .thenThrow(
                new RuntimeException(
                    "kafka down"
                )
            );

        // when & then
        assertThatCode(
            () ->
                riderReassignmentNotificationService.publish(
                    ASSIGNMENT_ID
                )
        )
            .doesNotThrowAnyException();

        verify(deliveryOperationNotificationProducer)
            .send(
                eq(RIDER_AUTH_USER_ID)
                , any(DeliveryOperationNotificationRequestedEvent.class)
            );

        verify(integrationEventRecordRepository)
            .save(
                any(IntegrationEventRecord.class)
            );
    }

    private DeliveryAssignment mockPublishableAssignment(
        LocalDate deliveryDate
        , DeliverySlotCode deliverySlotCode
    ) {
        DeliveryAssignment assignment =
            mockPublishableAssignmentUntilRider();

        Rider rider =
            assignment.getRider();

        DeliveryGroup deliveryGroup =
            assignment.getDeliveryGroup();

        DeliverySlot deliverySlot =
            mock(DeliverySlot.class);

        when(rider.getAuthUserId())
            .thenReturn(RIDER_AUTH_USER_ID);

        when(deliveryGroup.getDeliveryDate())
            .thenReturn(deliveryDate);

        when(deliveryGroup.getSlot())
            .thenReturn(deliverySlot);

        when(deliverySlot.getCode())
            .thenReturn(deliverySlotCode);

        return assignment;
    }

    private DeliveryAssignment mockPublishableAssignmentUntilRider() {
        DeliveryAssignment assignment =
            mock(DeliveryAssignment.class);

        Rider rider =
            mock(Rider.class);

        DeliveryGroup deliveryGroup =
            mock(DeliveryGroup.class);

        when(
            deliveryAssignmentRepository.findByIdAndDeletedAtIsNull(
                ASSIGNMENT_ID
            )
        )
            .thenReturn(
                Optional.of(assignment)
            );

        when(assignment.isAssigned())
            .thenReturn(true);

        when(assignment.getDeliveryGroup())
            .thenReturn(deliveryGroup);

        when(assignment.getRider())
            .thenReturn(rider);

        when(rider.getIsDeliveryActive())
            .thenReturn(true);

        when(assignment.getId())
            .thenReturn(ASSIGNMENT_ID);

        return assignment;
    }
}