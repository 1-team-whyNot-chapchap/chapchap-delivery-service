package com.chapchap.delivery.domain.access.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.entity.DeliveryAccessProfile;
import com.chapchap.delivery.domain.access.repository.DeliveryAccessProfileRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryAccessService {
    private final DeliveryAccessProfileRepository deliveryAccessProfileRepository;

    @Transactional(readOnly = true)
    public void validateAdminAccess(
        Long authUserId
        , UserRole gatewayRole
    ) {
        if (gatewayRole == null || !gatewayRole.isAdministrator()) {
            throw new DeliveryAccessForbiddenException();
        }

        // SUPER_ADMIN is always an active administrator in Auth and does not have a delivery projection.
        if (gatewayRole == UserRole.SUPER_ADMIN) {
            return;
        }

        deliveryAccessProfileRepository.findByAuthUserId(authUserId)
            .ifPresent(this::validateAdminProfile);
    }

    private void validateAdminProfile(
        DeliveryAccessProfile profile
    ) {
        if (
            profile.getLastRole() != UserRole.ADMIN
                || !Boolean.TRUE.equals(profile.getAccessAllowed())
        ) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    @Transactional(readOnly = true)
    public void validateRiderAccess(
        Long authUserId
        , UserRole gatewayRole
    ) {
        if (gatewayRole != UserRole.RIDER) {
            throw new DeliveryAccessForbiddenException();
        }

        DeliveryAccessProfile profile =
            deliveryAccessProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(DeliveryAccessForbiddenException::new);

        if (
            profile.getLastRole() != UserRole.RIDER
                || !Boolean.TRUE.equals(profile.getAccessAllowed())
        ) {
            throw new DeliveryAccessForbiddenException();
        }
    }

    @Transactional(readOnly = true)
    public boolean isRiderAccessAllowed(Long authUserId) {
        return deliveryAccessProfileRepository.findByAuthUserId(authUserId)
            .map(
                profile ->
                    profile.getLastRole() == UserRole.RIDER
                        && Boolean.TRUE.equals(profile.getAccessAllowed())
            )
            .orElse(false);
    }
}
