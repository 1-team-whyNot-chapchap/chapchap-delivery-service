package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentAcknowledgementReminderStage;
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
import java.time.OffsetDateTime;
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
class RiderAcknowledgementNotificationServiceTest {

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

    private RiderAcknowledgementNotificationService riderAcknowledgementNotificationService;

    @BeforeEach
    void setUp() {
        riderAcknowledgementNotificationService =
            new RiderAcknowledgementNotificationService(
                deliveryAssignmentRepository
                , integrationEventRecordRepository
                , deliveryOperationNotificationProducer
                , jsonMapper
                , "delivery-operation-notification"
            );
    }

    @Test
    @DisplayName("점심 배정 확인 가능 시각이 열리면 기사에게 확인 요청 알림을 발행한다")
    void publishOpenedPublishesLunchAcknowledgementNotification() throws Exception {
        // given
        LocalDate deliveryDate =
            LocalDate.of(2026, 9, 6);

        DeliveryAssignment assignment =
            mockPublishableAssignment(
                deliveryDate
                , DeliverySlotCode.LUNCH
            );

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderAcknowledgementOpened(
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
        riderAcknowledgementNotificationService.publishOpened(
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
                "RIDER_ACKNOWLEDGEMENT_OPENED"
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
                OffsetDateTime.parse(
                    "2026-09-06T09:00:00+09:00"
                )
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
    @DisplayName("저녁 배정 확인 요청 알림의 응답 기한은 당일 15시다")
    void publishOpenedUsesDinnerResponseDeadline() throws Exception {
        // given
        LocalDate deliveryDate =
            LocalDate.of(2026, 9, 6);

        mockPublishableAssignment(
            deliveryDate
            , DeliverySlotCode.DINNER
        );

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderAcknowledgementOpened(
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
        riderAcknowledgementNotificationService.publishOpened(
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
                OffsetDateTime.parse(
                    "2026-09-06T15:00:00+09:00"
                )
            );

        verify(integrationEventRecordRepository)
            .save(
                any(IntegrationEventRecord.class)
            );
    }

    @Test
    @DisplayName("동일한 확인 오픈 알림 businessKey가 이미 처리됐으면 다시 발행하지 않는다")
    void publishOpenedDoesNotPublishWhenBusinessKeyAlreadyExists() {
        // given
        mockPublishableAssignmentUntilRider();

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderAcknowledgementOpened(
                ASSIGNMENT_ID
            );

        when(
            integrationEventRecordRepository.existsByBusinessKey(
                businessKey
            )
        )
            .thenReturn(true);

        // when
        riderAcknowledgementNotificationService.publishOpened(
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
    @DisplayName("배정이 ASSIGNED 상태가 아니면 확인 오픈 알림을 발행하지 않는다")
    void publishOpenedDoesNotPublishWhenAssignmentIsNotAssigned() {
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
        riderAcknowledgementNotificationService.publishOpened(
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
    @DisplayName("배송 비활성 기사에게는 확인 오픈 알림을 발행하지 않는다")
    void publishOpenedDoesNotPublishWhenRiderIsNotDeliveryActive() {
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
        riderAcknowledgementNotificationService.publishOpened(
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
    @DisplayName("배정이 존재하지 않으면 확인 오픈 알림을 발행하지 않는다")
    void publishOpenedDoesNotPublishWhenAssignmentDoesNotExist() {
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
        riderAcknowledgementNotificationService.publishOpened(
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
    @DisplayName("Kafka 발행에 실패하면 예외를 전파하지 않고 실패 Integration Event를 저장한다")
    void publishOpenedSavesFailedIntegrationEventWhenKafkaPublishFails() throws Exception {
        // given
        mockPublishableAssignment(
            LocalDate.of(2026, 9, 6)
            , DeliverySlotCode.LUNCH
        );

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderAcknowledgementOpened(
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
                riderAcknowledgementNotificationService.publishOpened(
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

    @Test
    @DisplayName("FIRST 리마인더를 발행하면 FIRST businessKey와 reminderStage를 사용한다")
    void publishReminderPublishesFirstReminder() throws Exception {
        // given
        LocalDate deliveryDate =
            LocalDate.of(2026, 9, 6);

        mockPublishableAssignment(
            deliveryDate
            , DeliverySlotCode.LUNCH
        );

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderAcknowledgementReminder(
                ASSIGNMENT_ID
                , DeliveryAssignmentAcknowledgementReminderStage.FIRST.name()
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
        riderAcknowledgementNotificationService.publishReminder(
            ASSIGNMENT_ID
            , DeliveryAssignmentAcknowledgementReminderStage.FIRST
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

        assertThat(event.data().notificationType())
            .isEqualTo(
                "RIDER_ACKNOWLEDGEMENT_REMINDER"
            );

        assertThat(event.data().businessKey())
            .isEqualTo(
                businessKey
            );

        assertThat(event.data().reminderStage())
            .isEqualTo(
                "FIRST"
            );

        assertThat(event.data().responseDeadline())
            .isEqualTo(
                OffsetDateTime.parse(
                    "2026-09-06T09:00:00+09:00"
                )
            );

        verify(integrationEventRecordRepository)
            .save(
                any(IntegrationEventRecord.class)
            );
    }

    @Test
    @DisplayName("FINAL 리마인더를 발행하면 FINAL businessKey와 reminderStage를 사용한다")
    void publishReminderPublishesFinalReminder() throws Exception {
        // given
        LocalDate deliveryDate =
            LocalDate.of(2026, 9, 6);

        mockPublishableAssignment(
            deliveryDate
            , DeliverySlotCode.DINNER
        );

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderAcknowledgementReminder(
                ASSIGNMENT_ID
                , DeliveryAssignmentAcknowledgementReminderStage.FINAL.name()
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
        riderAcknowledgementNotificationService.publishReminder(
            ASSIGNMENT_ID
            , DeliveryAssignmentAcknowledgementReminderStage.FINAL
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

        assertThat(event.data().notificationType())
            .isEqualTo(
                "RIDER_ACKNOWLEDGEMENT_REMINDER"
            );

        assertThat(event.data().businessKey())
            .isEqualTo(
                businessKey
            );

        assertThat(event.data().reminderStage())
            .isEqualTo(
                "FINAL"
            );

        assertThat(event.data().responseDeadline())
            .isEqualTo(
                OffsetDateTime.parse(
                    "2026-09-06T15:00:00+09:00"
                )
            );

        verify(integrationEventRecordRepository)
            .save(
                any(IntegrationEventRecord.class)
            );
    }

    @Test
    @DisplayName("동일한 리마인더 businessKey가 이미 존재하면 다시 발행하지 않는다")
    void publishReminderDoesNotPublishWhenBusinessKeyAlreadyExists() {
        // given
        mockPublishableAssignmentUntilRider();

        String businessKey =
            DeliveryOperationNotificationBusinessKey.riderAcknowledgementReminder(
                ASSIGNMENT_ID
                , DeliveryAssignmentAcknowledgementReminderStage.FIRST.name()
            );

        when(
            integrationEventRecordRepository.existsByBusinessKey(
                businessKey
            )
        )
            .thenReturn(true);

        // when
        riderAcknowledgementNotificationService.publishReminder(
            ASSIGNMENT_ID
            , DeliveryAssignmentAcknowledgementReminderStage.FIRST
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
    @DisplayName("ASSIGNED 상태가 아니면 ACK 리마인더를 발행하지 않는다")
    void publishReminderDoesNotPublishWhenAssignmentIsNotAssigned() {
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
        riderAcknowledgementNotificationService.publishReminder(
            ASSIGNMENT_ID
            , DeliveryAssignmentAcknowledgementReminderStage.FINAL
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
}