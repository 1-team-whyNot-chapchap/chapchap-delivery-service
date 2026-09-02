package com.chapchap.delivery.global.kafka.validator;

import com.chapchap.delivery.global.kafka.event.SubscriptionDeliveryOrderReadyEvent;
import com.chapchap.delivery.global.exception.technical.InvalidSubscriptionDeliveryOrderEventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionDeliveryOrderReadyEventValidatorTest {

    private SubscriptionDeliveryOrderReadyEventValidator validator;

    @BeforeEach
    void setUp() {
        validator =
            new SubscriptionDeliveryOrderReadyEventValidator();
    }

    @Test
    void 정상_주문_Event를_검증한다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createValidEvent();

        assertDoesNotThrow(
            () -> validator.validate(
                event.data().orderId()
                , event
            )
        );
    }

    @Test
    void 지원하는_Event_Type이면_true를_반환한다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createValidEvent();

        assertTrue(validator.supports(event));
    }

    @Test
    void 지원하지_않는_Event_Type이면_false를_반환한다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createEvent(
                "UNKNOWN_EVENT"
                , createValidData()
            );

        assertFalse(validator.supports(event));
    }

    @Test
    void Kafka_Key와_orderId가_다르면_예외가_발생한다() {
        SubscriptionDeliveryOrderReadyEvent event =
            createValidEvent();

        assertThrows(
            InvalidSubscriptionDeliveryOrderEventException.class
            , () -> validator.validate(
                "different-order-id"
                , event
            )
        );
    }

    @Test
    void 지원하지_않는_version이면_예외가_발생한다() {
        SubscriptionDeliveryOrderReadyEvent valid =
            createValidEvent();

        SubscriptionDeliveryOrderReadyEvent event =
            new SubscriptionDeliveryOrderReadyEvent(
                valid.eventId()
                , valid.eventType()
                , 2
                , valid.occurredAt()
                , valid.userId()
                , valid.data()
            );

        assertThrows(
            InvalidSubscriptionDeliveryOrderEventException.class
            , () -> validator.validate(
                event.data().orderId()
                , event
            )
        );
    }

    @Test
    void 도시락_수량이_1보다_작으면_예외가_발생한다() {
        SubscriptionDeliveryOrderReadyEvent.Data data =
            createData(
                0
                , "DOORSTEP"
                , null
                , createMenuItems(0)
            );

        SubscriptionDeliveryOrderReadyEvent event =
            createEvent(
                "SUBSCRIPTION_DELIVERY_ORDER_READY"
                , data
            );

        assertThrows(
            InvalidSubscriptionDeliveryOrderEventException.class
            , () -> validator.validate(
                data.orderId()
                , event
            )
        );
    }

    @Test
    void menuItems가_한_개가_아니면_예외가_발생한다() {
        SubscriptionDeliveryOrderReadyEvent.Data data =
            createData(
                2
                , "DOORSTEP"
                , null
                , List.of()
            );

        SubscriptionDeliveryOrderReadyEvent event =
            createEvent(
                "SUBSCRIPTION_DELIVERY_ORDER_READY"
                , data
            );

        assertThrows(
            InvalidSubscriptionDeliveryOrderEventException.class
            , () -> validator.validate(
                data.orderId()
                , event
            )
        );
    }

    @Test
    void 도시락_수량과_메뉴_수량이_다르면_예외가_발생한다() {
        SubscriptionDeliveryOrderReadyEvent.Data data =
            createData(
                2
                , "DOORSTEP"
                , null
                , createMenuItems(1)
            );

        SubscriptionDeliveryOrderReadyEvent event =
            createEvent(
                "SUBSCRIPTION_DELIVERY_ORDER_READY"
                , data
            );

        assertThrows(
            InvalidSubscriptionDeliveryOrderEventException.class
            , () -> validator.validate(
                data.orderId()
                , event
            )
        );
    }

    @Test
    void OTHER인데_상세_요청이_없으면_예외가_발생한다() {
        SubscriptionDeliveryOrderReadyEvent.Data data =
            createData(
                2
                , "OTHER"
                , null
                , createMenuItems(2)
            );

        SubscriptionDeliveryOrderReadyEvent event =
            createEvent(
                "SUBSCRIPTION_DELIVERY_ORDER_READY"
                , data
            );

        assertThrows(
            InvalidSubscriptionDeliveryOrderEventException.class
            , () -> validator.validate(
                data.orderId()
                , event
            )
        );
    }

    @Test
    void OTHER이고_상세_요청이_있으면_정상이다() {
        SubscriptionDeliveryOrderReadyEvent.Data data =
            createData(
                2
                , "OTHER"
                , "경비실에 보관"
                , createMenuItems(2)
            );

        SubscriptionDeliveryOrderReadyEvent event =
            createEvent(
                "SUBSCRIPTION_DELIVERY_ORDER_READY"
                , data
            );

        assertDoesNotThrow(
            () -> validator.validate(
                data.orderId()
                , event
            )
        );
    }

    @Test
    void 약관에_동의하지_않으면_예외가_발생한다() {
        SubscriptionDeliveryOrderReadyEvent.Data valid =
            createValidData();

        SubscriptionDeliveryOrderReadyEvent.Data data =
            new SubscriptionDeliveryOrderReadyEvent.Data(
                valid.orderId()
                , valid.deliveryDate()
                , valid.deliverySlot()
                , valid.lunchboxQuantity()
                , valid.recipientName()
                , valid.recipientPhone()
                , valid.postalCode()
                , valid.addressLine1()
                , valid.addressLine2()
                , valid.deliveryMethod()
                , valid.deliveryMethodDetail()
                , valid.entranceInformation()
                , false
                , valid.termsAgreedAt()
                , valid.menuItems()
            );

        SubscriptionDeliveryOrderReadyEvent event =
            createEvent(
                "SUBSCRIPTION_DELIVERY_ORDER_READY"
                , data
            );

        assertThrows(
            InvalidSubscriptionDeliveryOrderEventException.class
            , () -> validator.validate(
                data.orderId()
                , event
            )
        );
    }

    @Test
    void 주소에서_시군구를_추출할_수_없으면_예외가_발생한다() {
        SubscriptionDeliveryOrderReadyEvent.Data valid =
            createValidData();

        SubscriptionDeliveryOrderReadyEvent.Data data =
            new SubscriptionDeliveryOrderReadyEvent.Data(
                valid.orderId()
                , valid.deliveryDate()
                , valid.deliverySlot()
                , valid.lunchboxQuantity()
                , valid.recipientName()
                , valid.recipientPhone()
                , valid.postalCode()
                , "대구광역시"
                , valid.addressLine2()
                , valid.deliveryMethod()
                , valid.deliveryMethodDetail()
                , valid.entranceInformation()
                , valid.termsAgreed()
                , valid.termsAgreedAt()
                , valid.menuItems()
            );

        SubscriptionDeliveryOrderReadyEvent event =
            createEvent(
                "SUBSCRIPTION_DELIVERY_ORDER_READY"
                , data
            );

        assertThrows(
            InvalidSubscriptionDeliveryOrderEventException.class
            , () -> validator.validate(
                data.orderId()
                , event
            )
        );
    }

    private SubscriptionDeliveryOrderReadyEvent createValidEvent() {
        return createEvent(
            "SUBSCRIPTION_DELIVERY_ORDER_READY"
            , createValidData()
        );
    }

    private SubscriptionDeliveryOrderReadyEvent createEvent(
        String eventType
        , SubscriptionDeliveryOrderReadyEvent.Data data
    ) {
        return new SubscriptionDeliveryOrderReadyEvent(
            UUID.randomUUID().toString()
            , eventType
            , 1
            , OffsetDateTime.parse(
            "2026-09-01T15:00:00+09:00"
        )
            , 25L
            , data
        );
    }

    private SubscriptionDeliveryOrderReadyEvent.Data createValidData() {
        return createData(
            2
            , "DOORSTEP"
            , null
            , createMenuItems(2)
        );
    }

    private SubscriptionDeliveryOrderReadyEvent.Data createData(
        Integer lunchboxQuantity
        , String deliveryMethod
        , String deliveryMethodDetail
        , List<SubscriptionDeliveryOrderReadyEvent.MenuItem> menuItems
    ) {
        return new SubscriptionDeliveryOrderReadyEvent.Data(
            UUID.randomUUID().toString()
            , LocalDate.of(2026, 9, 2)
            , "LUNCH"
            , lunchboxQuantity
            , "홍길동"
            , "010-1234-5678"
            , "41911"
            , "대구광역시 중구 국채보상로 1"
            , "101동 1001호"
            , deliveryMethod
            , deliveryMethodDetail
            , "공동현관 1234"
            , true
            , OffsetDateTime.parse(
            "2026-08-31T10:00:00+09:00"
        )
            , menuItems
        );
    }

    private List<SubscriptionDeliveryOrderReadyEvent.MenuItem>
    createMenuItems(
        int quantity
    ) {
        return List.of(
            new SubscriptionDeliveryOrderReadyEvent.MenuItem(
                UUID.randomUUID().toString()
                , "닭가슴살 도시락"
                , quantity
            )
        );
    }
}