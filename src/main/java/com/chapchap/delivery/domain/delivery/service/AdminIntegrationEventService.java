package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.constant.IntegrationEventStatus;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.domain.delivery.response.AdminIntegrationEventItemResponse;
import com.chapchap.delivery.domain.delivery.response.AdminIntegrationEventListResponse;
import com.chapchap.delivery.domain.delivery.response.AdminIntegrationEventRepublishResponse;
import com.chapchap.delivery.global.exception.business.IntegrationEventNotFoundException;
import com.chapchap.delivery.global.exception.business.IntegrationEventNotRepublishableException;
import com.chapchap.delivery.global.exception.business.InvalidRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminIntegrationEventService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_PAGE_SIZE = 100;
    private static final String PUBLISH_ERROR_CODE = "KAFKA_EVENT_PUBLISH_FAILED";
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
        "lastAttemptedAt", "lastAttemptedAt",
        "eventType", "eventType",
        "status", "status"
    );

    private final DeliveryAccessService accessService;
    private final IntegrationEventRecordRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Transactional(readOnly = true)
    public AdminIntegrationEventListResponse getEvents(
        Long actorId, UserRole role,
        IntegrationEventDirection direction, IntegrationEventStatus status,
        String eventType, OffsetDateTime from, OffsetDateTime to, Pageable pageable
    ) {
        accessService.validateAdminAccess(actorId, role);
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException();
        }
        return AdminIntegrationEventListResponse.from(
            repository.findAllForAdmin(
                direction, status, eventType, local(from), local(to), normalize(pageable)
            ).map(this::toItem)
        );
    }

    public AdminIntegrationEventRepublishResponse republish(
        Long actorId, UserRole role, Long recordId
    ) {
        accessService.validateAdminAccess(actorId, role);

        RepublishAttempt attempt = beginRepublish(recordId);
        try {
            kafkaTemplate.send(
                attempt.topic(), attempt.eventKey(),
                objectMapper.readTree(attempt.payloadJson())
            ).join();
            return finishRepublishSuccess(recordId);
        } catch (RuntimeException | JsonProcessingException exception) {
            return finishRepublishFailure(recordId, exception);
        }
    }

    private RepublishAttempt beginRepublish(Long recordId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        RepublishAttempt result = transactionTemplate.execute(status -> {
            IntegrationEventRecord record = repository.findByIdForUpdate(recordId)
                .orElseThrow(IntegrationEventNotFoundException::new);
            if (!record.isRepublishable()) {
                throw new IntegrationEventNotRepublishableException();
            }

            LocalDateTime attemptedAt = LocalDateTime.now(KST);
            record.markRepublishAttempt(attemptedAt);
            return new RepublishAttempt(
                record.getTopic(), record.getEventKey(), record.getPayloadJson()
            );
        });
        if (result == null) {
            throw new IllegalStateException("Could not start integration event republish");
        }
        return result;
    }

    private AdminIntegrationEventRepublishResponse finishRepublishSuccess(Long recordId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AdminIntegrationEventRepublishResponse result = transactionTemplate.execute(status -> {
            IntegrationEventRecord record = repository.findByIdForUpdate(recordId)
                .orElseThrow(IntegrationEventNotFoundException::new);
            record.markRepublishSuccess(LocalDateTime.now(KST));
            return toRepublishResponse(record);
        });
        if (result == null) {
            throw new IllegalStateException("Could not finish integration event republish");
        }
        return result;
    }

    private AdminIntegrationEventRepublishResponse finishRepublishFailure(
        Long recordId, Throwable exception
    ) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AdminIntegrationEventRepublishResponse result = transactionTemplate.execute(status -> {
            IntegrationEventRecord record = repository.findByIdForUpdate(recordId)
                .orElseThrow(IntegrationEventNotFoundException::new);
            record.markRepublishFailure(PUBLISH_ERROR_CODE, safeMessage(exception));
            return toRepublishResponse(record);
        });
        if (result == null) {
            throw new IllegalStateException("Could not finish integration event republish failure");
        }
        return result;
    }

    private AdminIntegrationEventRepublishResponse toRepublishResponse(
        IntegrationEventRecord record
    ) {
        return new AdminIntegrationEventRepublishResponse(
            record.getId(), record.getEventId(), record.getStatus(),
            record.getAttemptCount(), offset(record.getLastAttemptedAt())
        );
    }

    private AdminIntegrationEventItemResponse toItem(IntegrationEventRecord record) {
        return new AdminIntegrationEventItemResponse(
            record.getId(), record.getEventId(), record.getDirection(), record.getStatus(),
            record.getEventType(), record.getAggregateType(), record.getAggregateId(),
            record.getTopic(), record.getEventKey(), record.getAttemptCount(),
            offset(record.getLastAttemptedAt()), record.getErrorCode(),
            safePublicMessage(record.getErrorMessage())
        );
    }

    private Pageable normalize(Pageable pageable) {
        List<Sort.Order> orders = pageable.getSort().stream()
            .filter(order -> SORT_PROPERTIES.containsKey(order.getProperty()))
            .map(order -> new Sort.Order(
                order.getDirection(), SORT_PROPERTIES.get(order.getProperty())
            )).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (orders.isEmpty()) {
            orders.add(Sort.Order.desc("lastAttemptedAt"));
        }
        orders.add(Sort.Order.asc("id"));
        return PageRequest.of(
            pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
            Sort.by(orders)
        );
    }

    private LocalDateTime local(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(KST).toLocalDateTime();
    }

    private OffsetDateTime offset(LocalDateTime value) {
        return value == null ? null : value.atZone(KST).toOffsetDateTime();
    }

    private String safeMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return safePublicMessage(cause.getMessage() == null
            ? cause.getClass().getSimpleName() : cause.getMessage());
    }

    private String safePublicMessage(String message) {
        if (message == null) {
            return null;
        }
        String redacted = message
            .replaceAll("[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}", "[REDACTED]")
            .replaceAll("(?<!\\d)01[016789][- ]?\\d{3,4}[- ]?\\d{4}(?!\\d)", "[REDACTED]");
        return redacted.substring(0, Math.min(redacted.length(), 500));
    }

    private record RepublishAttempt(
        String topic, String eventKey, String payloadJson
    ) {
    }
}
