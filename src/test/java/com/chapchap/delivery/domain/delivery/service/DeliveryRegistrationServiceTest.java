package com.chapchap.delivery.domain.delivery.service;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryAreaCode;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroupStatusHistory;
import com.chapchap.delivery.domain.delivery.entity.DeliveryRecipientSnapshot;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import com.chapchap.delivery.domain.delivery.entity.DeliveryStatusHistory;
import com.chapchap.delivery.domain.delivery.entity.IntegrationEventRecord;
import com.chapchap.delivery.global.exception.business.DeliveryRegistrationException;
import com.chapchap.delivery.domain.delivery.repository.DeliveryAreaCodeRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryGroupStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRecipientSnapshotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliverySlotRepository;
import com.chapchap.delivery.domain.delivery.repository.DeliveryStatusHistoryRepository;
import com.chapchap.delivery.domain.delivery.repository.IntegrationEventRecordRepository;
import com.chapchap.delivery.global.kafka.event.SubscriptionDeliveryOrderReadyEvent;
import com.chapchap.delivery.global.security.PersonalDataEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeliveryRegistrationServiceTest {

    @Mock
    private DeliverySlotRepository deliverySlotRepository;

    @Mock
    private DeliveryAreaCodeRepository deliveryAreaCodeRepository;

    @Mock
    private DeliveryGroupRepository deliveryGroupRepository;

    @Mock
    private DeliveryGroupStatusHistoryRepository
        deliveryGroupStatusHistoryRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryRecipientSnapshotRepository
        deliveryRecipientSnapshotRepository;

    @Mock
    private DeliveryStatusHistoryRepository
        deliveryStatusHistoryRepository;

    @Mock
    private IntegrationEventRecordRepository
        integrationEventRecordRepository;

    @Mock
    private PersonalDataEncryptor personalDataEncryptor;

    private DeliveryRegistrationService deliveryRegistrationService;

    @BeforeEach
    void setUp() {
        deliveryRegistrationService =
            new DeliveryRegistrationService(
                deliverySlotRepository
                , deliveryAreaCodeRepository
                , deliveryGroupRepository
                , deliveryGroupStatusHistoryRepository
                , deliveryRepository
                , deliveryRecipientSnapshotRepository
                , deliveryStatusHistoryRepository
                , integrationEventRecordRepository
                , personalDataEncryptor
            );
    }

    @Test
    void 동일한_eventId가_이미_처리되었으면_아무것도_생성하지_않는다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createValidEvent();

        given(
            integrationEventRecordRepository.existsByEventId(
                event.eventId()
            )
        ).willReturn(true);

        assertDoesNotThrow(
            () -> deliveryRegistrationService.register(event)
        );

        verify(
            integrationEventRecordRepository
        ).existsByEventId(event.eventId());

        verifyNoMoreInteractions(
            integrationEventRecordRepository
        );

        verifyNoInteractions(
            deliverySlotRepository
            , deliveryAreaCodeRepository
            , deliveryGroupRepository
            , deliveryGroupStatusHistoryRepository
            , deliveryRepository
            , deliveryRecipientSnapshotRepository
            , deliveryStatusHistoryRepository
            , personalDataEncryptor
        );
    }

    @Test
    void 동일한_orderId가_이미_등록되어_있으면_배송을_중복_생성하지_않는다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createValidEvent();

        given(
            integrationEventRecordRepository.existsByEventId(
                event.eventId()
            )
        ).willReturn(false);

        given(
            deliveryRepository.countBySourceOrderIdIncludingDeleted(
                event.data().orderId()
            )
        ).willReturn(1L);

        assertDoesNotThrow(
            () -> deliveryRegistrationService.register(event)
        );

        verify(deliveryRepository)
            .countBySourceOrderIdIncludingDeleted(
                event.data().orderId()
            );

        verify(deliveryRepository, never())
            .save(any(Delivery.class));

        verify(integrationEventRecordRepository)
            .save(any(IntegrationEventRecord.class));

        verifyNoInteractions(
            deliverySlotRepository
            , deliveryAreaCodeRepository
            , deliveryGroupRepository
            , deliveryGroupStatusHistoryRepository
            , deliveryRecipientSnapshotRepository
            , deliveryStatusHistoryRepository
            , personalDataEncryptor
        );
    }

    @Test
    void 기존_전체배송이_있으면_새_그룹을_만들지_않고_재사용한다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createValidEvent();

        DeliverySlot slot = mockDeliverySlot();
        DeliveryAreaCode areaCode = mockDeliveryAreaCode();
        DeliveryGroup existingGroup = mockDeliveryGroup();

        prepareNewOrder(
            event
            , slot
            , areaCode
        );

        given(
            deliveryGroupRepository.findByDeliveryDateAndSlot(
                event.data().deliveryDate()
                , slot
            )
        ).willReturn(Optional.of(existingGroup));

        given(
            personalDataEncryptor.encrypt(
                event.data().recipientPhone()
            )
        ).willReturn(new byte[]{1, 2, 3});

        given(
            personalDataEncryptor.encrypt(
                event.data().entranceInformation()
            )
        ).willReturn(new byte[]{4, 5, 6});

        assertDoesNotThrow(
            () -> deliveryRegistrationService.register(event)
        );

        verify(deliveryGroupRepository, never())
            .save(any(DeliveryGroup.class));

        verify(deliveryGroupStatusHistoryRepository, never())
            .save(any(DeliveryGroupStatusHistory.class));

        verify(deliveryRepository)
            .save(any(Delivery.class));

        verify(deliveryRecipientSnapshotRepository)
            .save(any(DeliveryRecipientSnapshot.class));

        verify(deliveryStatusHistoryRepository)
            .save(any(DeliveryStatusHistory.class));

        verify(integrationEventRecordRepository)
            .save(any(IntegrationEventRecord.class));
    }

    @Test
    void 전체배송이_없으면_새_그룹과_초기_상태이력을_생성한다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createValidEvent();

        DeliverySlot slot = mockDeliverySlot();
        DeliveryAreaCode areaCode = mockDeliveryAreaCode();

        prepareNewOrder(
            event
            , slot
            , areaCode
        );

        given(
            deliveryGroupRepository.findByDeliveryDateAndSlot(
                event.data().deliveryDate()
                , slot
            )
        ).willReturn(Optional.empty());

        given(
            personalDataEncryptor.encrypt(
                event.data().recipientPhone()
            )
        ).willReturn(new byte[]{1, 2, 3});

        given(
            personalDataEncryptor.encrypt(
                event.data().entranceInformation()
            )
        ).willReturn(new byte[]{4, 5, 6});

        assertDoesNotThrow(
            () -> deliveryRegistrationService.register(event)
        );

        verify(deliveryGroupRepository)
            .save(any(DeliveryGroup.class));

        verify(deliveryGroupStatusHistoryRepository)
            .save(any(DeliveryGroupStatusHistory.class));

        verify(deliveryRepository)
            .save(any(Delivery.class));

        verify(deliveryRecipientSnapshotRepository)
            .save(any(DeliveryRecipientSnapshot.class));

        verify(deliveryStatusHistoryRepository)
            .save(any(DeliveryStatusHistory.class));

        verify(integrationEventRecordRepository)
            .save(any(IntegrationEventRecord.class));
    }

    @Test
    void 활성_배송지역을_찾지_못하면_배송을_등록하지_않는다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createValidEvent();

        DeliverySlot slot = mockDeliverySlot();

        given(
            integrationEventRecordRepository.existsByEventId(
                event.eventId()
            )
        ).willReturn(false);

        given(
            deliveryRepository.countBySourceOrderIdIncludingDeleted(
                event.data().orderId()
            )
        ).willReturn(0L);

        given(
            deliverySlotRepository.findByCodeAndDeletedAtIsNull(any())
        ).willReturn(Optional.of(slot));

        given(
            deliveryAreaCodeRepository
                .findByDistrictAndIsActiveTrue("중구")
        ).willReturn(Optional.empty());

        assertThrows(
            DeliveryRegistrationException.class
            , () -> deliveryRegistrationService.register(event)
        );

        verify(deliveryRepository, never())
            .save(any(Delivery.class));

        verify(deliveryRecipientSnapshotRepository, never())
            .save(any(DeliveryRecipientSnapshot.class));

        verify(deliveryStatusHistoryRepository, never())
            .save(any(DeliveryStatusHistory.class));

        verify(integrationEventRecordRepository, never())
            .save(any(IntegrationEventRecord.class));
    }

    @Test
    void 개인정보는_암호화한_후_수령정보를_저장한다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createValidEvent();

        DeliverySlot slot = mockDeliverySlot();
        DeliveryAreaCode areaCode = mockDeliveryAreaCode();
        DeliveryGroup group = mockDeliveryGroup();

        prepareNewOrder(
            event
            , slot
            , areaCode
        );

        given(
            deliveryGroupRepository.findByDeliveryDateAndSlot(
                event.data().deliveryDate()
                , slot
            )
        ).willReturn(Optional.of(group));

        given(
            personalDataEncryptor.encrypt(
                event.data().recipientPhone()
            )
        ).willReturn(new byte[]{1});

        given(
            personalDataEncryptor.encrypt(
                event.data().entranceInformation()
            )
        ).willReturn(new byte[]{2});

        deliveryRegistrationService.register(event);

        verify(personalDataEncryptor)
            .encrypt(event.data().recipientPhone());

        verify(personalDataEncryptor)
            .encrypt(event.data().entranceInformation());

        verify(deliveryRecipientSnapshotRepository)
            .save(any(DeliveryRecipientSnapshot.class));
    }

    private void prepareNewOrder(
        SubscriptionDeliveryOrderReadyEvent event
        , DeliverySlot slot
        , DeliveryAreaCode areaCode
    ) {
        given(
            integrationEventRecordRepository.existsByEventId(
                event.eventId()
            )
        ).willReturn(false);

        given(
            deliveryRepository.countBySourceOrderIdIncludingDeleted(
                event.data().orderId()
            )
        ).willReturn(0L);

        given(
            deliverySlotRepository.findByCodeAndDeletedAtIsNull(any())
        ).willReturn(Optional.of(slot));

        given(
            deliveryAreaCodeRepository
                .findByDistrictAndIsActiveTrue("중구")
        ).willReturn(Optional.of(areaCode));
    }

    private DeliverySlot mockDeliverySlot() {
        return org.mockito.Mockito.mock(DeliverySlot.class);
    }

    private DeliveryGroup mockDeliveryGroup() {
        return org.mockito.Mockito.mock(DeliveryGroup.class);
    }

    private DeliveryAreaCode mockDeliveryAreaCode() {
        DeliveryAreaCode areaCode =
            org.mockito.Mockito.mock(DeliveryAreaCode.class);

        given(areaCode.getAreaCode())
            .willReturn("DAEGU_JUNG");

        return areaCode;
    }

    private SubscriptionDeliveryOrderReadyEvent createValidEvent() {
        SubscriptionDeliveryOrderReadyEvent.MenuItem menuItem =
            new SubscriptionDeliveryOrderReadyEvent.MenuItem(
                UUID.randomUUID().toString()
                , "닭가슴살 도시락"
                , 2
            );

        SubscriptionDeliveryOrderReadyEvent.Data data =
            new SubscriptionDeliveryOrderReadyEvent.Data(
                UUID.randomUUID().toString()
                , LocalDate.of(2026, 9, 2)
                , "LUNCH"
                , 2
                , "홍길동"
                , "010-1234-5678"
                , "41911"
                , "대구광역시 중구 국채보상로 1"
                , "101동 1001호"
                , "DOORSTEP"
                , null
                , "공동현관 1234"
                , true
                , OffsetDateTime.parse(
                "2026-08-31T10:00:00+09:00"
            )
                , List.of(menuItem)
            );

        return new SubscriptionDeliveryOrderReadyEvent(
            UUID.randomUUID().toString()
            , "SUBSCRIPTION_DELIVERY_ORDER_READY"
            , 1
            , OffsetDateTime.parse(
            "2026-09-01T15:00:00+09:00"
        )
            , 25L
            , data
        );
    }
}