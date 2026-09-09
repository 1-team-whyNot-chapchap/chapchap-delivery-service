package com.chapchap.delivery.domain.access.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.entity.DeliveryAccessProfile;
import com.chapchap.delivery.domain.access.repository.DeliveryAccessProfileRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryAccessServiceRiderAccessTest {

    private static final Long AUTH_USER_ID = 10001L;

    @Mock
    private DeliveryAccessProfileRepository deliveryAccessProfileRepository;

    @Test
    @DisplayName("Gateway 역할과 Projection 역할이 RIDER이고 접근이 허용되어 있으면 통과한다")
    void validateRiderAccess() {
        // given
        DeliveryAccessService service = createService();
        DeliveryAccessProfile profile = createProfile(UserRole.RIDER, true);

        when(deliveryAccessProfileRepository.findByAuthUserId(AUTH_USER_ID))
            .thenReturn(Optional.of(profile));

        // when & then
        assertThatCode(
            () -> service.validateRiderAccess(AUTH_USER_ID, UserRole.RIDER)
        ).doesNotThrowAnyException();

        verify(deliveryAccessProfileRepository).findByAuthUserId(AUTH_USER_ID);
    }

    @Test
    @DisplayName("Gateway 역할이 RIDER가 아니면 접근을 거부한다")
    void validateRiderAccessThrowsExceptionWhenGatewayRoleIsNotRider() {
        // given
        DeliveryAccessService service = createService();

        // when & then
        assertThatThrownBy(
            () -> service.validateRiderAccess(AUTH_USER_ID, UserRole.CUSTOMER)
        ).isInstanceOf(DeliveryAccessForbiddenException.class);

        verify(deliveryAccessProfileRepository, never()).findByAuthUserId(AUTH_USER_ID);
    }

    @Test
    @DisplayName("RIDER 역할이지만 Delivery 접근 Projection이 없으면 접근을 거부한다")
    void validateRiderAccessThrowsExceptionWhenProfileNotFound() {
        // given
        DeliveryAccessService service = createService();

        when(deliveryAccessProfileRepository.findByAuthUserId(AUTH_USER_ID))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
            () -> service.validateRiderAccess(AUTH_USER_ID, UserRole.RIDER)
        ).isInstanceOf(DeliveryAccessForbiddenException.class);

        verify(deliveryAccessProfileRepository).findByAuthUserId(AUTH_USER_ID);
    }

    @Test
    @DisplayName("Gateway는 RIDER지만 Projection의 마지막 역할이 RIDER가 아니면 접근을 거부한다")
    void validateRiderAccessThrowsExceptionWhenProjectedRoleIsNotRider() {
        // given
        DeliveryAccessService service = createService();
        DeliveryAccessProfile profile = createProfile(UserRole.CUSTOMER);

        when(deliveryAccessProfileRepository.findByAuthUserId(AUTH_USER_ID))
            .thenReturn(Optional.of(profile));

        // when & then
        assertThatThrownBy(
            () -> service.validateRiderAccess(AUTH_USER_ID, UserRole.RIDER)
        ).isInstanceOf(DeliveryAccessForbiddenException.class);
    }

    @Test
    @DisplayName("RIDER Projection의 Delivery 접근이 비활성화되어 있으면 접근을 거부한다")
    void validateRiderAccessThrowsExceptionWhenAccessNotAllowed() {
        // given
        DeliveryAccessService service = createService();
        DeliveryAccessProfile profile = createProfile(UserRole.RIDER, false);

        when(deliveryAccessProfileRepository.findByAuthUserId(AUTH_USER_ID))
            .thenReturn(Optional.of(profile));

        // when & then
        assertThatThrownBy(
            () -> service.validateRiderAccess(AUTH_USER_ID, UserRole.RIDER)
        ).isInstanceOf(DeliveryAccessForbiddenException.class);
    }

    private DeliveryAccessService createService() {
        return new DeliveryAccessService(deliveryAccessProfileRepository);
    }

    private DeliveryAccessProfile createProfile(UserRole lastRole) {
        DeliveryAccessProfile profile = mock(DeliveryAccessProfile.class);

        when(profile.getLastRole()).thenReturn(lastRole);

        return profile;
    }

    private DeliveryAccessProfile createProfile(
        UserRole lastRole
        , Boolean accessAllowed
    ) {
        DeliveryAccessProfile profile = mock(DeliveryAccessProfile.class);

        when(profile.getLastRole()).thenReturn(lastRole);
        when(profile.getAccessAllowed()).thenReturn(accessAllowed);

        return profile;
    }
}