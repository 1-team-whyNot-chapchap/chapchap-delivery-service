package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.global.exception.business.IntegrationEventNotRepublishableException;
import com.chapchap.delivery.global.exception.business.InvalidRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminIntegrationEventServiceTest {
    @Mock private DeliveryAccessService accessService;
    @Mock private IntegrationEventRecordRepository repository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private RecordingTransactionManager transactionManager;
    private AdminIntegrationEventService service;

    @BeforeEach
    void setUp() {
        transactionManager = new RecordingTransactionManager();
        service = new AdminIntegrationEventService(
            accessService, repository, kafkaTemplate, new ObjectMapper(), transactionManager
        );
    }

    @Test
    @DisplayName("FAILED PUBLISH 기록을 DB 락 해제 후 동일 topic/key/payload로 재발행한다")
    void republishSuccessDoesNotHoldDbTransactionDuringKafkaSend() {
        IntegrationEventRecord record = failedRecord();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(record));
        when(kafkaTemplate.send(eq("topic-a"), eq("key-a"), any()))
            .thenAnswer(invocation -> {
                assertThat(transactionManager.active).isFalse();
                return CompletableFuture.<SendResult<String, Object>>completedFuture(null);
            });

        var response = service.republish(7L, UserRole.ADMIN, 1L);

        assertThat(response.status()).isEqualTo(IntegrationEventStatus.SUCCESS);
        assertThat(response.attemptCount()).isEqualTo(2);
        assertThat(record.getEventId()).isEqualTo("event-1");
        assertThat(record.getTopic()).isEqualTo("topic-a");
        assertThat(record.getEventKey()).isEqualTo("key-a");
        assertThat(record.getPayloadJson()).isEqualTo("{\"hello\":\"world\"}");
        verify(repository, org.mockito.Mockito.times(2)).findByIdForUpdate(1L);
    }

    @Test
    @DisplayName("재발행 실패도 실제 시도 횟수를 먼저 증가시키고 FAILED 상태를 유지한다")
    void republishFailureKeepsFailedAndCountsAttempt() {
        IntegrationEventRecord record = failedRecord();
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(record));
        CompletableFuture<SendResult<String, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(eq("topic-a"), eq("key-a"), any())).thenReturn(failed);

        var response = service.republish(7L, UserRole.ADMIN, 1L);

        assertThat(response.status()).isEqualTo(IntegrationEventStatus.FAILED);
        assertThat(response.attemptCount()).isEqualTo(2);
        assertThat(record.getErrorCode()).isEqualTo("KAFKA_EVENT_PUBLISH_FAILED");
        assertThat(record.getErrorMessage()).contains("broker down");
    }

    @Test
    @DisplayName("이미 성공한 Event는 재발행할 수 없다")
    void successRecordIsNotRepublishable() {
        IntegrationEventRecord record = IntegrationEventRecord.publishSuccess(
            "event-1", "TYPE", "DELIVERY", "delivery-1", "business-1",
            "topic-a", "key-a", "{\"hello\":\"world\"}",
            LocalDateTime.now(), LocalDateTime.now()
        );
        ReflectionTestUtils.setField(record, "id", 1L);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.republish(7L, UserRole.ADMIN, 1L))
            .isInstanceOf(IntegrationEventNotRepublishableException.class);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("한 번 성공한 기록은 늦게 도착한 실패 결과로 FAILED로 되돌아가지 않는다")
    void lateFailureCannotOverwriteSuccess() {
        IntegrationEventRecord record = failedRecord();
        record.markRepublishAttempt(LocalDateTime.now());
        record.markRepublishSuccess(LocalDateTime.now());

        record.markRepublishFailure("KAFKA_EVENT_PUBLISH_FAILED", "late failure");

        assertThat(record.getStatus()).isEqualTo(IntegrationEventStatus.SUCCESS);
        assertThat(record.getErrorCode()).isNull();
        assertThat(record.getAttemptCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("이벤트 조회 from이 to보다 늦으면 400 계열 BusinessException으로 거절한다")
    void invalidDateRangeIsRejectedBeforeRepositoryQuery() {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-07T12:00:00+09:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-07T11:00:00+09:00");

        assertThatThrownBy(() -> service.getEvents(
            7L, UserRole.ADMIN, null, null, null, from, to, PageRequest.of(0, 20)
        )).isInstanceOf(InvalidRequestException.class);

        verify(repository, never()).findAllForAdmin(any(), any(), any(), any(), any(), any());
    }

    private IntegrationEventRecord failedRecord() {
        IntegrationEventRecord record = IntegrationEventRecord.publishFailed(
            "event-1", "TYPE", "DELIVERY", "delivery-1", "business-1",
            "topic-a", "key-a", "{\"hello\":\"world\"}",
            LocalDateTime.of(2026, 9, 7, 10, 0),
            LocalDateTime.of(2026, 9, 7, 10, 1),
            "KAFKA_EVENT_PUBLISH_FAILED", "initial failure"
        );
        ReflectionTestUtils.setField(record, "id", 1L);
        return record;
    }

    private static final class RecordingTransactionManager implements PlatformTransactionManager {
        private boolean active;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            active = true;
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            active = false;
        }

        @Override
        public void rollback(TransactionStatus status) {
            active = false;
        }
    }
}
