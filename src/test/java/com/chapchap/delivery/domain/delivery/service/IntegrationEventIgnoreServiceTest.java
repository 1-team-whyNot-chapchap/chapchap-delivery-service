package com.chapchap.delivery.domain.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.chapchap.delivery.domain.delivery.constant.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationEventIgnoreServiceTest {
    @Mock private IntegrationEventRecordRepository repository;
    private IntegrationEventIgnoreService service;

    @BeforeEach
    void setUp() {
        service = new IntegrationEventIgnoreService(repository);
    }

    @Test
    @DisplayName("지원하지 않는 Event는 CONSUME IGNORED 처리 기록만 생성한다")
    void recordsIgnoredEvent() {
        String eventId = "0198a8e8-2acd-7b24-a682-b50c6784515d";
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-09-07T12:00:00+09:00");
        when(repository.existsByEventId(eventId)).thenReturn(false);

        service.ignore(eventId, "USER_REGISTERED", "AUTH_USER", "25", occurredAt);

        ArgumentCaptor<IntegrationEventRecord> captor =
            ArgumentCaptor.forClass(IntegrationEventRecord.class);
        verify(repository).save(captor.capture());
        IntegrationEventRecord record = captor.getValue();
        assertThat(record.getDirection()).isEqualTo(IntegrationEventDirection.CONSUME);
        assertThat(record.getStatus()).isEqualTo(IntegrationEventStatus.IGNORED);
        assertThat(record.getEventType()).isEqualTo("USER_REGISTERED");
        assertThat(record.getErrorCode()).isEqualTo(
            IntegrationEventIgnoreService.EVENT_TYPE_IGNORED
        );
    }

    @Test
    @DisplayName("이미 기록된 무시 Event는 중복 기록하지 않는다")
    void ignoresDuplicateRecord() {
        String eventId = "0198a8e8-2acd-7b24-a682-b50c6784515e";
        when(repository.existsByEventId(eventId)).thenReturn(true);

        service.ignore(
            eventId, "UNKNOWN_EVENT", "ORDER", null,
            OffsetDateTime.parse("2026-09-07T12:00:00+09:00")
        );

        verify(repository, never()).save(any());
        verifyNoMoreInteractions(repository);
    }
}
