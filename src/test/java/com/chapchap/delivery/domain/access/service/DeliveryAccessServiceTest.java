package com.chapchap.delivery.domain.access.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.entity.DeliveryAccessProfile;
import com.chapchap.delivery.domain.access.repository.DeliveryAccessProfileRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryAccessServiceTest {

    private static final Long ADMIN_USER_ID = 9001L;

    @Mock
    private DeliveryAccessProfileRepository
        deliveryAccessProfileRepository;

    @InjectMocks
    private DeliveryAccessService deliveryAccessService;

    @Test
    @DisplayName("Gateway 역할이 ADMIN이 아니면 접근을 거절한다")
    void validateAdminAccessGatewayRoleNotAdmin() {
        assertThrows(
            DeliveryAccessForbiddenException.class
            , () -> deliveryAccessService.validateAdminAccess(
                ADMIN_USER_ID
                , UserRole.RIDER
            )
        );

        verify(
            deliveryAccessProfileRepository
            , never()
        ).findByAuthUserId(ADMIN_USER_ID);
    }

    @Test
    @DisplayName("Gateway 역할이 ADMIN이고 로컬 Projection이 없으면 접근을 허용한다")
    void validateAdminAccessWithoutProfileSuccess() {
        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                ADMIN_USER_ID
            )
        ).thenReturn(Optional.empty());

        assertDoesNotThrow(
            () -> deliveryAccessService.validateAdminAccess(
                ADMIN_USER_ID
                , UserRole.ADMIN
            )
        );

        verify(
            deliveryAccessProfileRepository
        ).findByAuthUserId(ADMIN_USER_ID);
    }

    @Test
    @DisplayName("ADMIN Projection이 접근 허용 상태이면 접근을 허용한다")
    void validateAdminAccessAllowedProfileSuccess() {
        DeliveryAccessProfile profile =
            new DeliveryAccessProfile(
                ADMIN_USER_ID
                , UserRole.ADMIN
                , true
                , LocalDateTime.of(
                2026
                , 9
                , 3
                , 9
                , 0
            )
            );

        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                ADMIN_USER_ID
            )
        ).thenReturn(Optional.of(profile));

        assertDoesNotThrow(
            () -> deliveryAccessService.validateAdminAccess(
                ADMIN_USER_ID
                , UserRole.ADMIN
            )
        );
    }

    @Test
    @DisplayName("ADMIN Projection이 접근 차단 상태이면 접근을 거절한다")
    void validateAdminAccessBlockedProfile() {
        DeliveryAccessProfile profile =
            new DeliveryAccessProfile(
                ADMIN_USER_ID
                , UserRole.ADMIN
                , false
                , LocalDateTime.of(
                2026
                , 9
                , 3
                , 9
                , 0
            )
            );

        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                ADMIN_USER_ID
            )
        ).thenReturn(Optional.of(profile));

        assertThrows(
            DeliveryAccessForbiddenException.class
            , () -> deliveryAccessService.validateAdminAccess(
                ADMIN_USER_ID
                , UserRole.ADMIN
            )
        );
    }

    @Test
    @DisplayName("로컬 Projection의 마지막 역할이 ADMIN이 아니면 접근을 거절한다")
    void validateAdminAccessProfileRoleMismatch() {
        DeliveryAccessProfile profile =
            new DeliveryAccessProfile(
                ADMIN_USER_ID
                , UserRole.RIDER
                , true
                , LocalDateTime.of(
                2026
                , 9
                , 3
                , 9
                , 0
            )
            );

        when(
            deliveryAccessProfileRepository.findByAuthUserId(
                ADMIN_USER_ID
            )
        ).thenReturn(Optional.of(profile));

        assertThrows(
            DeliveryAccessForbiddenException.class
            , () -> deliveryAccessService.validateAdminAccess(
                ADMIN_USER_ID
                , UserRole.ADMIN
            )
        );
    }
}