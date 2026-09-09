package com.chapchap.delivery.domain.audit.entity;

import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_histories")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "actor_id")
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10)
    private AuditActorType actorType;

    @Column(name = "reason_code", length = 32)
    private String reasonCode;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_value_json", columnDefinition = "JSON")
    private String beforeValueJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_value_json", columnDefinition = "JSON")
    private String afterValueJson;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AuditHistory record(
        String entityType
        , Long entityId
        , String action
        , Long actorId
        , AuditActorType actorType
        , String reasonCode
        , String reasonDetail
        , String beforeValueJson
        , String afterValueJson
        , LocalDateTime occurredAt
    ) {
        AuditHistory history = new AuditHistory();

        history.entityType = entityType;
        history.entityId = entityId;
        history.action = action;
        history.actorId = actorId;
        history.actorType = actorType;
        history.reasonCode = reasonCode;
        history.reasonDetail = reasonDetail;
        history.beforeValueJson = beforeValueJson;
        history.afterValueJson = afterValueJson;
        history.occurredAt = occurredAt;

        return history;
    }
}