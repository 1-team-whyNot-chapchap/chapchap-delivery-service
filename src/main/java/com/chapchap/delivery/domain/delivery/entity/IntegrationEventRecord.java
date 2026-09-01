package com.chapchap.delivery.domain.delivery.entity;

import com.chapchap.delivery.domain.delivery.type.IntegrationEventDirection;
import com.chapchap.delivery.domain.delivery.type.IntegrationEventStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "integration_event_records"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_integration_event_records_event_id"
            , columnNames = "event_id"
        )
        , @UniqueConstraint(
            name = "uk_integration_event_records_business_key"
            , columnNames = "business_key"
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntegrationEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IntegrationEventDirection direction;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    @Column(name = "business_key", length = 200)
    private String businessKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IntegrationEventStatus status;

    @Column(length = 200)
    private String topic;

    @Column(name = "event_key", length = 200)
    private String eventKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "JSON")
    private String payloadJson;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static IntegrationEventRecord consumeSuccess(
        String eventId
        , String eventType
        , String aggregateType
        , String aggregateId
        , LocalDateTime occurredAt
        , LocalDateTime processedAt
    ) {
        IntegrationEventRecord record = new IntegrationEventRecord();

        record.eventId = eventId;
        record.direction = IntegrationEventDirection.CONSUME;
        record.eventType = eventType;
        record.aggregateType = aggregateType;
        record.aggregateId = aggregateId;
        record.status = IntegrationEventStatus.SUCCESS;
        record.occurredAt = occurredAt;
        record.processedAt = processedAt;

        return record;
    }
}