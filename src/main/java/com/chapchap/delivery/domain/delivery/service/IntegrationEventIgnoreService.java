package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntegrationEventIgnoreService {
    public static final String EVENT_TYPE_IGNORED =
        "DELIVERY_KAFKA_EVENT_TYPE_IGNORED";

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final IntegrationEventRecordRepository recordRepository;

    @Transactional
    public void ignore(
        String eventId
        , String eventType
        , String aggregateType
        , String aggregateId
        , OffsetDateTime occurredAt
    ) {
        if (recordRepository.existsByEventId(eventId)) {
            return;
        }
        LocalDateTime processedAt = LocalDateTime.now(KST);
        recordRepository.save(
            IntegrationEventRecord.consumeIgnored(
                eventId
                , eventType
                , aggregateType
                , aggregateId
                , occurredAt.atZoneSameInstant(KST).toLocalDateTime()
                , processedAt
                , EVENT_TYPE_IGNORED
            )
        );
    }
}
