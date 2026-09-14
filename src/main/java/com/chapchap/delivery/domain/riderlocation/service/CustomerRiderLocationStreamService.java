package com.chapchap.delivery.domain.riderlocation.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.assignment.constant.DeliveryAssignmentStatus;
import com.chapchap.delivery.domain.assignment.repository.DeliveryAssignmentItemRepository;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.riderlocation.repository.RiderCurrentLocationRepository;
import com.chapchap.delivery.domain.riderlocation.response.RiderLocationResponse;
import com.chapchap.delivery.global.config.RiderLocationProperties;
import com.chapchap.delivery.global.exception.business.DeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.DeliveryNotFoundException;
import com.chapchap.delivery.global.exception.business.DeliveryStateConflictException;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class CustomerRiderLocationStreamService {
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAssignmentItemRepository assignmentItemRepository;
    private final RiderCurrentLocationRepository currentLocationRepository;
    private final RiderLocationSseRegistry registry;
    private final RiderLocationProperties properties;
    private final Clock kstClock;

    @Transactional(readOnly = true)
    public SseEmitter open(Long customerId, UserRole role, String deliveryPublicId) throws IOException {
        if (role != UserRole.CUSTOMER) throw new DeliveryAccessForbiddenException();
        Delivery delivery = deliveryRepository.findDetailByDeliveryPublicId(deliveryPublicId)
            .orElseThrow(DeliveryNotFoundException::new);
        if (!delivery.getCustomerId().equals(customerId)) throw new DeliveryNotFoundException();
        if (delivery.getStatus() != DeliveryStatus.DELIVERING) throw new DeliveryStateConflictException();
        Long riderId = assignmentItemRepository.findConfirmedRiderIdsByDeliveryId(
            delivery.getId(), DeliveryAssignmentStatus.CONFIRMED
        ).stream().findFirst().orElseThrow(DeliveryStateConflictException::new);

        SseEmitter emitter = registry.subscribe(deliveryPublicId, properties.sseTimeout().toMillis());
        currentLocationRepository.findById(riderId)
            .filter(location -> !location.getCapturedAt().isBefore(
                LocalDateTime.now(kstClock).minus(properties.maxPositionAge())
            ))
            .ifPresent(location -> {
                try {
                    emitter.send(SseEmitter.event().name("rider-location").data(RiderLocationResponse.from(location)));
                } catch (IOException exception) {
                    emitter.completeWithError(exception);
                }
            });
        return emitter;
    }
}
