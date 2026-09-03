package com.chapchap.delivery.domain.access.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.entity.DeliveryAccessProfile;
import com.chapchap.delivery.domain.access.repository.DeliveryAccessProfileRepository;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.kafka.event.AuthUserEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthUserEventService {
    private static final ZoneId KST =
        ZoneId.of("Asia/Seoul");

    private static final String AUTH_USER_AGGREGATE_TYPE =
        "AUTH_USER";

    private static final String USER_ROLE_CHANGED =
        "USER_ROLE_CHANGED";

    private static final String USER_WITHDRAWN =
        "USER_WITHDRAWN";

    private static final String ADMIN_ACCOUNT_DISABLED =
        "ADMIN_ACCOUNT_DISABLED";

    private final DeliveryAccessProfileRepository
        deliveryAccessProfileRepository;

    private final RiderRepository riderRepository;

    private final IntegrationEventRecordRepository
        integrationEventRecordRepository;

    @Transactional
    public void process(AuthUserEvent event) {
        if (integrationEventRecordRepository.existsByEventId(
            event.eventId()
        )) {
            return;
        }

        LocalDateTime occurredAt =
            toKstLocalDateTime(event.occurredAt());

        LocalDateTime processedAt =
            LocalDateTime.now(KST);

        Optional<DeliveryAccessProfile> profileOptional =
            deliveryAccessProfileRepository.findByAuthUserId(
                event.userId()
            );

        if (
            profileOptional.isPresent()
                && !occurredAt.isAfter(
                profileOptional.get()
                    .getLastAuthEventOccurredAt()
            )
        ) {
            saveConsumedEvent(
                event
                , occurredAt
                , processedAt
            );

            return;
        }

        switch (event.eventType()) {
            case USER_ROLE_CHANGED ->
                handleUserRoleChanged(
                    event
                    , profileOptional
                    , occurredAt
                );

            case USER_WITHDRAWN ->
                handleUserWithdrawn(
                    profileOptional
                    , occurredAt
                );

            case ADMIN_ACCOUNT_DISABLED ->
                handleAdminAccountDisabled(
                    event
                    , profileOptional
                    , occurredAt
                );

            default -> {
                return;
            }
        }

        saveConsumedEvent(
            event
            , occurredAt
            , processedAt
        );
    }

    private void handleUserRoleChanged(
        AuthUserEvent event
        , Optional<DeliveryAccessProfile> profileOptional
        , LocalDateTime occurredAt
    ) {
        UserRole newRole =
            UserRole.valueOf(
                event.data().newRole()
            );

        boolean accessAllowed =
            newRole == UserRole.RIDER;

        if (profileOptional.isPresent()) {
            DeliveryAccessProfile profile =
                profileOptional.get();

            profile.updateAuthState(
                newRole
                , accessAllowed
                , occurredAt
            );
        } else {
            DeliveryAccessProfile profile =
                new DeliveryAccessProfile(
                    event.userId()
                    , newRole
                    , accessAllowed
                    , occurredAt
                );

            deliveryAccessProfileRepository.save(profile);
        }

        if (newRole == UserRole.RIDER) {
            createRiderIfAbsent(event.userId());
        }
    }

    private void handleUserWithdrawn(
        Optional<DeliveryAccessProfile> profileOptional
        , LocalDateTime occurredAt
    ) {
        if (profileOptional.isEmpty()) {
            return;
        }

        DeliveryAccessProfile profile =
            profileOptional.get();

        profile.updateAuthState(
            profile.getLastRole()
            , false
            , occurredAt
        );
    }

    private void handleAdminAccountDisabled(
        AuthUserEvent event
        , Optional<DeliveryAccessProfile> profileOptional
        , LocalDateTime occurredAt
    ) {
        if (profileOptional.isPresent()) {
            DeliveryAccessProfile profile =
                profileOptional.get();

            profile.updateAuthState(
                UserRole.ADMIN
                , false
                , occurredAt
            );

            return;
        }

        DeliveryAccessProfile profile =
            new DeliveryAccessProfile(
                event.userId()
                , UserRole.ADMIN
                , false
                , occurredAt
            );

        deliveryAccessProfileRepository.save(profile);
    }

    private void createRiderIfAbsent(Long authUserId) {
        if (riderRepository.findByAuthUserId(authUserId)
            .isPresent()) {
            return;
        }

        Rider rider =
            new Rider(authUserId);

        riderRepository.save(rider);
    }

    private void saveConsumedEvent(
        AuthUserEvent event
        , LocalDateTime occurredAt
        , LocalDateTime processedAt
    ) {
        IntegrationEventRecord record =
            IntegrationEventRecord.consumeSuccess(
                event.eventId()
                , event.eventType()
                , AUTH_USER_AGGREGATE_TYPE
                , String.valueOf(event.userId())
                , occurredAt
                , processedAt
            );

        integrationEventRecordRepository.save(record);
    }

    private LocalDateTime toKstLocalDateTime(
        OffsetDateTime dateTime
    ) {
        return dateTime
            .atZoneSameInstant(KST)
            .toLocalDateTime();
    }
}