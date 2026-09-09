package com.chapchap.delivery.domain.assignment.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.access.service.DeliveryAccessService;
import org.springframework.stereotype.Service;

@Service
public class AdminAutoAssignmentService {
    private final DeliveryAccessService deliveryAccessService;
    private final AutoAssignmentService autoAssignmentService;

    public AdminAutoAssignmentService(
        DeliveryAccessService deliveryAccessService
        , AutoAssignmentService autoAssignmentService
    ) {
        this.deliveryAccessService = deliveryAccessService;
        this.autoAssignmentService = autoAssignmentService;
    }

    public boolean assign(Long actorId, UserRole actorRole, Long deliveryGroupId) {
        deliveryAccessService.validateAdminAccess(actorId, actorRole);
        return autoAssignmentService.assign(deliveryGroupId);
    }
}
