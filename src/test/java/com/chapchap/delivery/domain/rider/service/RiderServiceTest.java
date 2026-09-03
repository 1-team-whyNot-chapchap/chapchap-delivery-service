package com.chapchap.delivery.domain.rider.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import com.chapchap.delivery.domain.audit.constant.AuditActorType;
import com.chapchap.delivery.domain.audit.entity.AuditHistory;
import com.chapchap.delivery.domain.audit.repository.AuditHistoryRepository;
import com.chapchap.delivery.domain.rider.constant.RiderDeliveryActiveReason;
import com.chapchap.delivery.domain.rider.entity.Rider;
import com.chapchap.delivery.domain.rider.repository.RiderRepository;
import com.chapchap.delivery.domain.rider.request.RiderUpdateRequest;
import com.chapchap.delivery.global.exception.business.OptimisticLockConflictException;
import com.chapchap.delivery.global.exception.business.OtherReasonDetailRequiredException;
import com.chapchap.delivery.global.exception.business.RiderNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderServiceTest {
    private static final Long RIDER_ID = 31L;
    private static final Long ADMIN_USER_ID = 9001L;

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private AuditHistoryRepository auditHistoryRepository;

    @Mock
    private DeliveryAccessService deliveryAccessService;

    @InjectMocks
    private RiderService riderService;

    @Test
    @DisplayName("기사 배송업무 상태를 false에서 true로 변경하고 감사 이력을 저장한다")
    void changeDeliveryActiveSuccess() {
        Rider rider =
            createRider(
                false
                , 3L
            );

        RiderUpdateRequest request =
            new RiderUpdateRequest(
                true
                , 3L
                , RiderDeliveryActiveReason.INITIAL_ACTIVATION
                , "  최초 배송업무 활성화  "
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(Optional.of(rider));

        riderService.changeDeliveryActive(
            RIDER_ID
            , ADMIN_USER_ID
            , UserRole.ADMIN
            , request
        );

        assertTrue(
            rider.getIsDeliveryActive()
        );

        verify(
            deliveryAccessService
        ).validateAdminAccess(
            ADMIN_USER_ID
            , UserRole.ADMIN
        );

        ArgumentCaptor<AuditHistory> auditCaptor =
            ArgumentCaptor.forClass(
                AuditHistory.class
            );

        verify(
            auditHistoryRepository
        ).save(auditCaptor.capture());

        AuditHistory auditHistory =
            auditCaptor.getValue();

        assertEquals(
            "RIDER"
            , auditHistory.getEntityType()
        );

        assertEquals(
            RIDER_ID
            , auditHistory.getEntityId()
        );

        assertEquals(
            "DELIVERY_ACTIVE_CHANGED"
            , auditHistory.getAction()
        );

        assertEquals(
            ADMIN_USER_ID
            , auditHistory.getActorId()
        );

        assertEquals(
            AuditActorType.ADMIN
            , auditHistory.getActorType()
        );

        assertEquals(
            "INITIAL_ACTIVATION"
            , auditHistory.getReasonCode()
        );

        assertEquals(
            "최초 배송업무 활성화"
            , auditHistory.getReasonDetail()
        );

        assertEquals(
            "{\"isDeliveryActive\":false}"
            , auditHistory.getBeforeValueJson()
        );

        assertEquals(
            "{\"isDeliveryActive\":true}"
            , auditHistory.getAfterValueJson()
        );
    }

    @Test
    @DisplayName("기사를 찾을 수 없으면 RiderNotFoundException이 발생한다")
    void changeDeliveryActiveRiderNotFound() {
        RiderUpdateRequest request =
            new RiderUpdateRequest(
                true
                , 0L
                , RiderDeliveryActiveReason.INITIAL_ACTIVATION
                , null
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(Optional.empty());

        assertThrows(
            RiderNotFoundException.class
            , () -> riderService.changeDeliveryActive(
                RIDER_ID
                , ADMIN_USER_ID
                , UserRole.ADMIN
                , request
            )
        );

        verify(
            deliveryAccessService
        ).validateAdminAccess(
            ADMIN_USER_ID
            , UserRole.ADMIN
        );

        verify(
            auditHistoryRepository
            , never()
        ).save(any(AuditHistory.class));
    }

    @Test
    @DisplayName("요청 version이 현재 기사 version과 다르면 충돌한다")
    void changeDeliveryActiveVersionConflict() {
        Rider rider =
            createRider(
                false
                , 4L
            );

        RiderUpdateRequest request =
            new RiderUpdateRequest(
                true
                , 3L
                , RiderDeliveryActiveReason.INITIAL_ACTIVATION
                , null
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(Optional.of(rider));

        assertThrows(
            OptimisticLockConflictException.class
            , () -> riderService.changeDeliveryActive(
                RIDER_ID
                , ADMIN_USER_ID
                , UserRole.ADMIN
                , request
            )
        );

        assertFalse(
            rider.getIsDeliveryActive()
        );

        verify(
            deliveryAccessService
        ).validateAdminAccess(
            ADMIN_USER_ID
            , UserRole.ADMIN
        );

        verify(
            auditHistoryRepository
            , never()
        ).save(any(AuditHistory.class));
    }

    @Test
    @DisplayName("OTHER 사유에 상세 설명이 없으면 변경을 거절한다")
    void changeDeliveryActiveOtherReasonDetailRequired() {
        Rider rider =
            createRider(
                true
                , 3L
            );

        RiderUpdateRequest request =
            new RiderUpdateRequest(
                false
                , 3L
                , RiderDeliveryActiveReason.OTHER
                , "   "
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(Optional.of(rider));

        assertThrows(
            OtherReasonDetailRequiredException.class
            , () -> riderService.changeDeliveryActive(
                RIDER_ID
                , ADMIN_USER_ID
                , UserRole.ADMIN
                , request
            )
        );

        assertTrue(
            rider.getIsDeliveryActive()
        );

        verify(
            deliveryAccessService
        ).validateAdminAccess(
            ADMIN_USER_ID
            , UserRole.ADMIN
        );

        verify(
            auditHistoryRepository
            , never()
        ).save(any(AuditHistory.class));
    }

    @Test
    @DisplayName("현재 상태와 같은 상태를 요청하면 변경과 감사 기록을 만들지 않는다")
    void changeDeliveryActiveSameStateNoOp() {
        Rider rider =
            createRider(
                true
                , 3L
            );

        RiderUpdateRequest request =
            new RiderUpdateRequest(
                true
                , 3L
                , RiderDeliveryActiveReason.RESUME_DELIVERY
                , null
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(Optional.of(rider));

        riderService.changeDeliveryActive(
            RIDER_ID
            , ADMIN_USER_ID
            , UserRole.ADMIN
            , request
        );

        assertTrue(
            rider.getIsDeliveryActive()
        );

        verify(
            deliveryAccessService
        ).validateAdminAccess(
            ADMIN_USER_ID
            , UserRole.ADMIN
        );

        verify(
            auditHistoryRepository
            , never()
        ).save(any(AuditHistory.class));
    }

    @Test
    @DisplayName("OTHER가 아니면 빈 reasonDetail을 null로 정규화해 감사 이력에 저장한다")
    void changeDeliveryActiveNormalizeBlankReasonDetail() {
        Rider rider =
            createRider(
                true
                , 3L
            );

        RiderUpdateRequest request =
            new RiderUpdateRequest(
                false
                , 3L
                , RiderDeliveryActiveReason.TRAINING
                , "   "
            );

        when(
            riderRepository.findByIdAndDeletedAtIsNull(
                RIDER_ID
            )
        ).thenReturn(Optional.of(rider));

        riderService.changeDeliveryActive(
            RIDER_ID
            , ADMIN_USER_ID
            , UserRole.ADMIN
            , request
        );

        verify(
            deliveryAccessService
        ).validateAdminAccess(
            ADMIN_USER_ID
            , UserRole.ADMIN
        );

        ArgumentCaptor<AuditHistory> auditCaptor =
            ArgumentCaptor.forClass(
                AuditHistory.class
            );

        verify(
            auditHistoryRepository
        ).save(auditCaptor.capture());

        AuditHistory auditHistory =
            auditCaptor.getValue();

        assertFalse(
            rider.getIsDeliveryActive()
        );

        assertEquals(
            "TRAINING"
            , auditHistory.getReasonCode()
        );

        assertNull(
            auditHistory.getReasonDetail()
        );
    }

    private Rider createRider(
        boolean isDeliveryActive
        , Long version
    ) {
        Rider rider =
            new Rider(25L);

        ReflectionTestUtils.setField(
            rider
            , "id"
            , RIDER_ID
        );

        ReflectionTestUtils.setField(
            rider
            , "version"
            , version
        );

        if (isDeliveryActive) {
            rider.changeDeliveryActive(true);
        }

        return rider;
    }
}