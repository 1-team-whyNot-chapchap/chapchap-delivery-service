package com.chapchap.delivery.domain.audit.response;

import com.chapchap.delivery.domain.audit.constant.AuditActorType;

import java.time.OffsetDateTime;

public record AdminAuditHistoryItemResponse(
    Long auditHistoryId
    , String entityType
    , Long entityId
    , String action
    , Long actorId
    , AuditActorType actorType
    , String reasonCode
    , String reasonDetail
    , OffsetDateTime occurredAt
) {
}
