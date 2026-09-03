package com.chapchap.delivery.global.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserEvent(
    String eventId
    , String eventType
    , Integer version
    , OffsetDateTime occurredAt
    , Long userId
    , Data data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
        String role
        , String previousRole
        , String newRole
        , OffsetDateTime withdrawnAt
        , OffsetDateTime disabledAt
    ) {
    }
}