package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryAreaCode;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.entity.DeliveryRecipientSnapshot;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.domain.delivery.exception.DeliveryRegistrationException;
import com.chapchap.delivery.domain.delivery.repository.DeliveryAreaCodeRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliverySlotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.domain.delivery.type.DeliveryChangedByType;
import com.chapchap.delivery.domain.delivery.type.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.type.DeliveryGroupStatus;
import com.chapchap.delivery.domain.delivery.type.DeliverySlotCode;
import com.chapchap.delivery.domain.delivery.type.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.type.RequestHandoffType;
import com.chapchap.delivery.global.kafka.event.SubscriptionDeliveryOrderReadyEvent;
import com.chapchap.delivery.global.security.PersonalDataEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryRegistrationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final String ORDER_AGGREGATE_TYPE = "ORDER";

    private final DeliverySlotRepository deliverySlotRepository;
    private final DeliveryAreaCodeRepository deliveryAreaCodeRepository;
    private final DeliveryGroupRepository deliveryGroupRepository;
    private final DeliveryGroupStatusHistoryRepository
        deliveryGroupStatusHistoryRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryRecipientSnapshotRepository
        deliveryRecipientSnapshotRepository;
    private final DeliveryStatusHistoryRepository
        deliveryStatusHistoryRepository;
    private final IntegrationEventRecordRepository
        integrationEventRecordRepository;
    private final PersonalDataEncryptor personalDataEncryptor;

    @Transactional
    public void register(
        SubscriptionDeliveryOrderReadyEvent event
    ) {
        if (integrationEventRecordRepository.existsByEventId(
            event.eventId()
        )) {
            return;
        }

        SubscriptionDeliveryOrderReadyEvent.Data data = event.data();

        LocalDateTime processedAt = LocalDateTime.now(KST);

        if (deliveryRepository.countBySourceOrderIdIncludingDeleted(data.orderId()) > 0) {
            saveConsumedEvent(
                event
                , data.orderId()
                , processedAt
            );

            return;
        }

        DeliverySlot slot = findDeliverySlot(data.deliverySlot());

        String normalizedAddress =
            normalizeAddress(data.addressLine1());

        String district =
            extractDistrict(normalizedAddress);

        DeliveryAreaCode deliveryAreaCode =
            findDeliveryAreaCode(district);

        DeliveryGroup deliveryGroup =
            findOrCreateDeliveryGroup(
                data
                , slot
                , processedAt
            );

        SubscriptionDeliveryOrderReadyEvent.MenuItem menuItem =
            data.menuItems().get(0);

        Delivery delivery = createDelivery(
            event
            , data
            , menuItem
            , deliveryGroup
            , deliveryAreaCode
        );

        deliveryRepository.save(delivery);

        saveRecipientSnapshot(
            delivery
            , data
            , normalizedAddress
        );

        saveInitialDeliveryStatusHistory(
            delivery
            , processedAt
        );

        saveConsumedEvent(
            event
            , data.orderId()
            , processedAt
        );
    }

    private DeliverySlot findDeliverySlot(String deliverySlot) {
        DeliverySlotCode slotCode =
            DeliverySlotCode.valueOf(deliverySlot);

        return deliverySlotRepository.findByCodeAndDeletedAtIsNull(slotCode)
            .orElseThrow(
                () -> new DeliveryRegistrationException(
                    "Delivery slot not found: " + slotCode
                )
            );
    }

    private DeliveryAreaCode findDeliveryAreaCode(
        String district
    ) {
        return deliveryAreaCodeRepository
            .findByDistrictAndIsActiveTrue(district)
            .orElseThrow(
                () -> new DeliveryRegistrationException(
                    "Active delivery area code not found."
                )
            );
    }

    private DeliveryGroup findOrCreateDeliveryGroup(
        SubscriptionDeliveryOrderReadyEvent.Data data
        , DeliverySlot slot
        , LocalDateTime processedAt
    ) {
        return deliveryGroupRepository
            .findByDeliveryDateAndSlot(
                data.deliveryDate()
                , slot
            )
            .orElseGet(
                () -> createDeliveryGroup(
                    data
                    , slot
                    , processedAt
                )
            );
    }

    private DeliveryGroup createDeliveryGroup(
        SubscriptionDeliveryOrderReadyEvent.Data data
        , DeliverySlot slot
        , LocalDateTime processedAt
    ) {
        DeliveryGroup deliveryGroup =
            new DeliveryGroup(
                data.deliveryDate()
                , slot
            );

        deliveryGroupRepository.save(deliveryGroup);

        DeliveryGroupStatusHistory history =
            new DeliveryGroupStatusHistory(
                deliveryGroup
                , null
                , DeliveryGroupStatus.WAITING_ASSIGNMENT
                , null
                , DeliveryGroupChangedByType.SYSTEM
                , processedAt
            );

        deliveryGroupStatusHistoryRepository.save(history);

        return deliveryGroup;
    }

    private Delivery createDelivery(
        SubscriptionDeliveryOrderReadyEvent event
        , SubscriptionDeliveryOrderReadyEvent.Data data
        , SubscriptionDeliveryOrderReadyEvent.MenuItem menuItem
        , DeliveryGroup deliveryGroup
        , DeliveryAreaCode deliveryAreaCode
    ) {
        return new Delivery(
            deliveryGroup
            , data.orderId()
            , UUID.randomUUID().toString()
            , event.userId()
            , deliveryAreaCode.getAreaCode()
            , data.lunchboxQuantity()
            , menuItem.menuId()
            , menuItem.menuName()
            , RequestHandoffType.valueOf(data.deliveryMethod())
            , toKstLocalDateTime(data.termsAgreedAt())
        );
    }

    private void saveRecipientSnapshot(
        Delivery delivery
        , SubscriptionDeliveryOrderReadyEvent.Data data
        , String normalizedAddress
    ) {
        byte[] encryptedPhone =
            personalDataEncryptor.encrypt(
                data.recipientPhone()
            );

        byte[] encryptedEntranceInformation =
            encryptNullable(
                data.entranceInformation()
            );

        String otherRequest =
            resolveOtherRequest(data);

        DeliveryRecipientSnapshot snapshot =
            new DeliveryRecipientSnapshot(
                delivery
                , data.recipientName()
                , encryptedPhone
                , data.postalCode()
                , normalizedAddress
                , data.addressLine2()
                , encryptedEntranceInformation
                , otherRequest
            );

        deliveryRecipientSnapshotRepository.save(snapshot);
    }

    private void saveInitialDeliveryStatusHistory(
        Delivery delivery
        , LocalDateTime processedAt
    ) {
        DeliveryStatusHistory history =
            new DeliveryStatusHistory(
                delivery
                , null
                , DeliveryStatus.READY
                , null
                , DeliveryChangedByType.SYSTEM
                , processedAt
            );

        deliveryStatusHistoryRepository.save(history);
    }

    private void saveConsumedEvent(
        SubscriptionDeliveryOrderReadyEvent event
        , String orderId
        , LocalDateTime processedAt
    ) {
        IntegrationEventRecord record =
            IntegrationEventRecord.consumeSuccess(
                event.eventId()
                , event.eventType()
                , ORDER_AGGREGATE_TYPE
                , orderId
                , toKstLocalDateTime(event.occurredAt())
                , processedAt
            );

        integrationEventRecordRepository.save(record);
    }

    private String normalizeAddress(String addressLine1) {
        return addressLine1
            .trim()
            .replaceAll("\\s+", " ");
    }

    private String extractDistrict(String normalizedAddress) {
        String[] addressParts =
            normalizedAddress.split(" ");

        if (addressParts.length < 2) {
            throw new DeliveryRegistrationException(
                "District cannot be extracted from address."
            );
        }

        return addressParts[1];
    }

    private String resolveOtherRequest(
        SubscriptionDeliveryOrderReadyEvent.Data data
    ) {
        if (!RequestHandoffType.OTHER.name()
            .equals(data.deliveryMethod())) {
            return null;
        }

        return data.deliveryMethodDetail();
    }

    private byte[] encryptNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return personalDataEncryptor.encrypt(value);
    }

    private LocalDateTime toKstLocalDateTime(
        java.time.OffsetDateTime dateTime
    ) {
        return dateTime
            .atZoneSameInstant(KST)
            .toLocalDateTime();
    }
}