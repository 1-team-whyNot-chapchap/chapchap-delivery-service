package com.chapchap.delivery.domain.riderlocation.service;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.riderlocation.event.DeliveryTrackingEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RiderLocationTrackingLifecycle {
    private final ApplicationEventPublisher publisher;

    public void deliveryEnded(Delivery delivery) {
        publisher.publishEvent(new DeliveryTrackingEndedEvent(
            delivery.getId(), delivery.getDeliveryPublicId(), delivery.getStatus()
        ));
    }
}
