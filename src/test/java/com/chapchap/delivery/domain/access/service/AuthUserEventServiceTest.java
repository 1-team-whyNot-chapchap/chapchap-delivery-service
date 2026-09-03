package com.chapchap.delivery.domain.access.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.entity.DeliveryAccessProfile;
import com.chapchap.delivery.domain.access.repository.DeliveryAccessProfileRepository;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.global.kafka.event.AuthUserEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserEventServiceTest {

    @Mock
    private DeliveryAccessProfileRepository
        deliveryAccessProfileRepository;

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private IntegrationEventRecordRepository
        integrationEventRecordRepository;

    @InjectMocks
    private AuthUserEventService authUserEventService;

    @Test
    @DisplayName("이미 처리한 eventId이면 아무 작업도 하지 않는다")
    void processDuplicateEventIgnore() {
        AuthUserEvent event =
            createRoleChangedEvent(
                "0198a8fa-533a-7251-a110-b1675f654358"
                , "CUSTOMER"
                , "RIDER"
            );

        when(
            integrationEventRecordRepository.existsByEventId(
                event.eventId()
            )
        ).thenReturn(true);

        authUserEventService.process(event);

        verify(
            integrationEventRecordRepository
        ).existsByEventId(event.eventId());

        verifyNoMoreInteractions(
            integrationEventRecordRepository
            , deliveryAccessProfileRepository
            , riderRepository
        );
    }

    @Test
    @DisplayName("CUSTOMER에서 RIDER로 변경되면 접근 Projection과 Rider를 생성한다")
    void processCustomerToRiderCreateProfileAndRider() {
        AuthUserEvent event =
            createRoleChangedEvent(
                "0198a8fa-533a-7251-a110-b1675f654358"
                , "CUSTOMER"
                , "RIDER"
            );

        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                event.userId()
            )
        ).thenReturn(Optional.empty());

        when(
            riderRepository.findByAuthUserId(
                event.userId()
            )
        ).thenReturn(Optional.empty());

        authUserEventService.process(event);

        ArgumentCaptor<DeliveryAccessProfile> profileCaptor =
            ArgumentCaptor.forClass(
                DeliveryAccessProfile.class
            );

        verify(
            deliveryAccessProfileRepository
        ).save(profileCaptor.capture());

        DeliveryAccessProfile savedProfile =
            profileCaptor.getValue();

        assertEquals(
            event.userId()
            , savedProfile.getAuthUserId()
        );

        assertEquals(
            UserRole.RIDER
            , savedProfile.getLastRole()
        );

        assertTrue(
            savedProfile.getAccessAllowed()
        );

        ArgumentCaptor<Rider> riderCaptor =
            ArgumentCaptor.forClass(Rider.class);

        verify(
            riderRepository
        ).save(riderCaptor.capture());

        Rider savedRider =
            riderCaptor.getValue();

        assertEquals(
            event.userId()
            , savedRider.getAuthUserId()
        );

        assertFalse(
            savedRider.getIsDeliveryActive()
        );

        verify(
            integrationEventRecordRepository
        ).save(any(IntegrationEventRecord.class));
    }

    @Test
    @DisplayName("RIDER에서 CUSTOMER로 변경되면 접근을 차단하고 Rider는 삭제하지 않는다")
    void processRiderToCustomerDisableAccess() {
        DeliveryAccessProfile profile =
            new DeliveryAccessProfile(
                25L
                , UserRole.RIDER
                , true
                , LocalDateTime.of(
                2026
                , 8
                , 16
                , 20
                , 0
            )
            );

        AuthUserEvent event =
            createRoleChangedEvent(
                "0198a8fb-533a-7251-a110-b1675f654359"
                , "RIDER"
                , "CUSTOMER"
            );

        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                25L
            )
        ).thenReturn(Optional.of(profile));

        authUserEventService.process(event);

        assertEquals(
            UserRole.CUSTOMER
            , profile.getLastRole()
        );

        assertFalse(
            profile.getAccessAllowed()
        );

        verify(
            riderRepository
            , never()
        ).save(any(Rider.class));

        verify(
            integrationEventRecordRepository
        ).save(any(IntegrationEventRecord.class));
    }

    @Test
    @DisplayName("저장된 occurredAt보다 오래된 역할 Event는 Projection을 변경하지 않는다")
    void processOlderEventIgnoreProjectionChange() {
        LocalDateTime latestOccurredAt =
            LocalDateTime.of(
                2026
                , 8
                , 16
                , 22
                , 0
            );

        DeliveryAccessProfile profile =
            new DeliveryAccessProfile(
                25L
                , UserRole.RIDER
                , true
                , latestOccurredAt
            );

        AuthUserEvent event =
            new AuthUserEvent(
                "0198a8fc-533a-7251-a110-b1675f654360"
                , "USER_ROLE_CHANGED"
                , 1
                , OffsetDateTime.parse(
                "2026-08-16T21:20:00+09:00"
            )
                , 25L
                , new AuthUserEvent.Data(
                null
                , "RIDER"
                , "CUSTOMER"
                , null
                , null
            )
            );

        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                25L
            )
        ).thenReturn(Optional.of(profile));

        authUserEventService.process(event);

        assertEquals(
            UserRole.RIDER
            , profile.getLastRole()
        );

        assertTrue(
            profile.getAccessAllowed()
        );

        assertEquals(
            latestOccurredAt
            , profile.getLastAuthEventOccurredAt()
        );

        verify(
            riderRepository
            , never()
        ).save(any(Rider.class));

        verify(
            deliveryAccessProfileRepository
            , never()
        ).save(any(DeliveryAccessProfile.class));

        verify(
            integrationEventRecordRepository
        ).save(any(IntegrationEventRecord.class));
    }

    @Test
    @DisplayName("회원 탈퇴 Event를 받으면 기존 역할을 유지하면서 접근을 차단한다")
    void processUserWithdrawnDisableAccess() {
        DeliveryAccessProfile profile =
            new DeliveryAccessProfile(
                25L
                , UserRole.RIDER
                , true
                , LocalDateTime.of(
                2026
                , 8
                , 16
                , 20
                , 0
            )
            );

        AuthUserEvent event =
            createUserWithdrawnEvent();

        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                25L
            )
        ).thenReturn(Optional.of(profile));

        authUserEventService.process(event);

        assertEquals(
            UserRole.RIDER
            , profile.getLastRole()
        );

        assertFalse(
            profile.getAccessAllowed()
        );

        assertEquals(
            LocalDateTime.of(
                2026
                , 8
                , 16
                , 21
                , 10
            )
            , profile.getLastAuthEventOccurredAt()
        );

        verify(
            integrationEventRecordRepository
        ).save(any(IntegrationEventRecord.class));
    }

    @Test
    @DisplayName("Delivery Projection이 없는 일반 사용자의 탈퇴 Event는 Projection을 만들지 않고 소비 완료 처리한다")
    void processUserWithdrawnWithoutProfile() {
        AuthUserEvent event =
            createUserWithdrawnEvent();

        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                25L
            )
        ).thenReturn(Optional.empty());

        authUserEventService.process(event);

        verify(
            deliveryAccessProfileRepository
            , never()
        ).save(any(DeliveryAccessProfile.class));

        verify(
            riderRepository
            , never()
        ).save(any(Rider.class));

        verify(
            integrationEventRecordRepository
        ).save(any(IntegrationEventRecord.class));
    }

    @Test
    @DisplayName("관리자 비활성 Event이고 Projection이 없으면 접근 불가 ADMIN Projection을 생성한다")
    void processAdminDisabledCreateProfile() {
        AuthUserEvent event =
            createAdminDisabledEvent();

        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                9001L
            )
        ).thenReturn(Optional.empty());

        authUserEventService.process(event);

        ArgumentCaptor<DeliveryAccessProfile> profileCaptor =
            ArgumentCaptor.forClass(
                DeliveryAccessProfile.class
            );

        verify(
            deliveryAccessProfileRepository
        ).save(profileCaptor.capture());

        DeliveryAccessProfile savedProfile =
            profileCaptor.getValue();

        assertEquals(
            9001L
            , savedProfile.getAuthUserId()
        );

        assertEquals(
            UserRole.ADMIN
            , savedProfile.getLastRole()
        );

        assertFalse(
            savedProfile.getAccessAllowed()
        );

        verify(
            integrationEventRecordRepository
        ).save(any(IntegrationEventRecord.class));
    }

    private AuthUserEvent createRoleChangedEvent(
        String eventId
        , String previousRole
        , String newRole
    ) {
        return new AuthUserEvent(
            eventId
            , "USER_ROLE_CHANGED"
            , 1
            , OffsetDateTime.parse(
            "2026-08-16T21:20:00+09:00"
        )
            , 25L
            , new AuthUserEvent.Data(
            null
            , previousRole
            , newRole
            , null
            , null
        )
        );
    }

    private AuthUserEvent createUserWithdrawnEvent() {
        OffsetDateTime withdrawnAt =
            OffsetDateTime.parse(
                "2026-08-16T21:10:00+09:00"
            );

        return new AuthUserEvent(
            "0198a8f1-7652-7f08-9a15-b2d921ff51d3"
            , "USER_WITHDRAWN"
            , 1
            , withdrawnAt
            , 25L
            , new AuthUserEvent.Data(
            null
            , null
            , null
            , withdrawnAt
            , null
        )
        );
    }

    private AuthUserEvent createAdminDisabledEvent() {
        OffsetDateTime disabledAt =
            OffsetDateTime.parse(
                "2026-08-16T21:30:00+09:00"
            );

        return new AuthUserEvent(
            "0198a903-6b41-7a2d-b036-49f20670e10b"
            , "ADMIN_ACCOUNT_DISABLED"
            , 1
            , disabledAt
            , 9001L
            , new AuthUserEvent.Data(
            null
            , null
            , null
            , null
            , disabledAt
        )
        );
    }
}