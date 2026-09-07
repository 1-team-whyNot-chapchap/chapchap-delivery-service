package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.access.constant.UserRole;
import com.chapchap.delivery.domain.delivery.constant.CurrentDeliveryDelayStatus;
import com.chapchap.delivery.domain.delivery.constant.CurrentDeliveryStatus;
import com.chapchap.delivery.domain.delivery.constant.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.repository.DeliveryDelayRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.response.CurrentDeliveryResponse;
import com.chapchap.delivery.global.exception.business.CurrentDeliveryAccessForbiddenException;
import com.chapchap.delivery.global.exception.business.CurrentDeliveryDataConflictException;
import com.chapchap.delivery.global.exception.business.CurrentDeliveryNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentDeliveryQueryService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeliveryRepository deliveryRepository;
    private final DeliveryDelayRepository delayRepository;
    private final DeliveryStatusHistoryRepository historyRepository;

    public CurrentDeliveryQueryService(
        DeliveryRepository deliveryRepository
        , DeliveryDelayRepository delayRepository
        , DeliveryStatusHistoryRepository historyRepository
    ) {
        this.deliveryRepository = deliveryRepository;
        this.delayRepository = delayRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public CurrentDeliveryResponse getCurrent(Long customerId, UserRole role) {
        if (role != UserRole.CUSTOMER) {
            throw new CurrentDeliveryAccessForbiddenException();
        }
        LocalDateTime now = LocalDateTime.now(KST);
        List<Delivery> deliveries = deliveryRepository.findAllForCurrentState(
            customerId, now.toLocalDate()
        );
        Delivery delivery = select(
            deliveries, now.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
        );
        LocalDateTime changedAt = historyRepository
            .findAllByDelivery_IdOrderByChangedAtAsc(delivery.getId()).stream()
            .filter(history -> history.getToStatus() == delivery.getStatus())
            .map(DeliveryStatusHistory::getChangedAt)
            .max(Comparator.naturalOrder())
            .orElse(null);
        return new CurrentDeliveryResponse(
            publicStatus(delivery.getStatus())
            , delayStatus(delivery, changedAt, now)
            , changedAt == null ? null : changedAt.atZone(KST).toOffsetDateTime()
        );
    }

    private Delivery select(List<Delivery> deliveries, LocalTime now) {
        List<Delivery> delivering = byStatus(deliveries, DeliveryStatus.DELIVERING);
        if (!delivering.isEmpty()) {
            return only(delivering);
        }
        List<Delivery> ready = deliveries.stream()
            .filter(delivery -> delivery.getStatus() == DeliveryStatus.READY)
            .filter(delivery -> !delivery.getDeliveryGroup().getSlot().getStartTime().isBefore(now))
            .toList();
        if (!ready.isEmpty()) {
            LocalTime nearest = ready.getFirst().getDeliveryGroup().getSlot().getStartTime();
            return only(ready.stream()
                .filter(delivery -> delivery.getDeliveryGroup().getSlot().getStartTime().equals(nearest))
                .toList());
        }
        List<Delivery> ended = deliveries.stream()
            .filter(delivery -> delivery.getStatus() == DeliveryStatus.DELIVERED
                || delivery.getStatus() == DeliveryStatus.FAILED)
            .sorted(Comparator.comparing(
                (Delivery delivery) -> delivery.getDeliveryGroup().getSlot().getStartTime()
            ).reversed())
            .toList();
        if (ended.isEmpty()) {
            throw new CurrentDeliveryNotFoundException();
        }
        LocalTime latest = ended.getFirst().getDeliveryGroup().getSlot().getStartTime();
        return only(ended.stream()
            .filter(delivery -> delivery.getDeliveryGroup().getSlot().getStartTime().equals(latest))
            .toList());
    }

    private List<Delivery> byStatus(List<Delivery> deliveries, DeliveryStatus status) {
        return deliveries.stream().filter(delivery -> delivery.getStatus() == status).toList();
    }

    private Delivery only(List<Delivery> deliveries) {
        if (deliveries.size() != 1) {
            throw new CurrentDeliveryDataConflictException();
        }
        return deliveries.getFirst();
    }

    private CurrentDeliveryStatus publicStatus(DeliveryStatus status) {
        return switch (status) {
            case READY -> CurrentDeliveryStatus.READY;
            case DELIVERING -> CurrentDeliveryStatus.DELIVERING;
            case DELIVERED -> CurrentDeliveryStatus.COMPLETED;
            case FAILED -> CurrentDeliveryStatus.FAILED;
        };
    }

    private CurrentDeliveryDelayStatus delayStatus(
        Delivery delivery, LocalDateTime changedAt, LocalDateTime now
    ) {
        if (delayRepository.findByDeliveryId(delivery.getId()).isPresent()) {
            return CurrentDeliveryDelayStatus.DELAYED;
        }
        LocalDateTime deadline = LocalDateTime.of(
            delivery.getDeliveryGroup().getDeliveryDate()
            , delivery.getDeliveryGroup().getSlot().getEndTime().plusMinutes(1)
        );
        if (delivery.getStatus() == DeliveryStatus.READY
            || delivery.getStatus() == DeliveryStatus.DELIVERING) {
            return now.isBefore(deadline)
                ? CurrentDeliveryDelayStatus.NOT_DELAYED
                : CurrentDeliveryDelayStatus.DELAYED;
        }
        if (changedAt == null) {
            return CurrentDeliveryDelayStatus.UNKNOWN;
        }
        return changedAt.isBefore(deadline)
            ? CurrentDeliveryDelayStatus.NOT_DELAYED
            : CurrentDeliveryDelayStatus.DELAYED;
    }
}
