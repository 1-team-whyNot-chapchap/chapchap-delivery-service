package com.chapchap.delivery.domain.access.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.entity.DeliveryAccessProfile;
import com.chapchap.delivery.domain.access.repository.DeliveryAccessProfileRepository;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryAccessService {
    private final DeliveryAccessProfileRepository deliveryAccessProfileRepository;

    @Transactional(readOnly = true)
    public void validateAdminAccess(
        Long authUserId
        , UserRole gatewayRole
    ) {
        if (gatewayRole != UserRole.ADMIN) {
            throw new DeliveryAccessForbiddenException();
        }

        Optional<DeliveryAccessProfile> profileOptional =
            deliveryAccessProfileRepository.findByAuthUserId(authUserId);

        if (profileOptional.isEmpty()) {
            return;
        }

        DeliveryAccessProfile profile = profileOptional.get();

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