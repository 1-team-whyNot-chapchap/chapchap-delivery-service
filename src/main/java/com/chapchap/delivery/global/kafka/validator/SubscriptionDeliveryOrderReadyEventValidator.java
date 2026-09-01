package com.chapchap.delivery.global.kafka.validator;

import com.chapchap.delivery.global.kafka.event.SubscriptionDeliveryOrderReadyEvent;
import com.chapchap.delivery.global.kafka.exception.InvalidSubscriptionDeliveryOrderEventException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class SubscriptionDeliveryOrderReadyEventValidator {

    private static final String SUPPORTED_EVENT_TYPE =
        "SUBSCRIPTION_DELIVERY_ORDER_READY";

    private static final int SUPPORTED_VERSION = 1;

    private static final Set<String> SUPPORTED_DELIVERY_SLOTS =
        Set.of("LUNCH", "DINNER");

    private static final Set<String> SUPPORTED_DELIVERY_METHODS =
        Set.of("DIRECT", "DOORSTEP", "OTHER");

    private static final String OTHER_DELIVERY_METHOD = "OTHER";

    public boolean supports(SubscriptionDeliveryOrderReadyEvent event) {
        return event != null
            && SUPPORTED_EVENT_TYPE.equals(event.eventType());
    }

    public void validate(
        String messageKey
        , SubscriptionDeliveryOrderReadyEvent event
    ) {
        validateEnvelope(event);

        SubscriptionDeliveryOrderReadyEvent.Data data = event.data();

        validateOrder(messageKey, data);
        validateRecipient(data);
        validateDeliveryMethod(data);
        validateTerms(data);
        validateMenu(data);
        validateAddressStructure(data.addressLine1());
    }

    private void validateEnvelope(
        SubscriptionDeliveryOrderReadyEvent event
    ) {
        if (event == null) {
            throw invalid("event");
        }

        if (!SUPPORTED_EVENT_TYPE.equals(event.eventType())) {
            throw invalid("eventType");
        }

        if (event.version() == null
            || event.version() != SUPPORTED_VERSION) {
            throw invalid("version");
        }

        requireNotBlank(event.eventId(), "eventId");
        validateEventId(event.eventId());

        if (event.occurredAt() == null) {
            throw invalid("occurredAt");
        }

        if (event.userId() == null || event.userId() <= 0) {
            throw invalid("userId");
        }

        if (event.data() == null) {
            throw invalid("data");
        }
    }

    private void validateOrder(
        String messageKey
        , SubscriptionDeliveryOrderReadyEvent.Data data
    ) {
        requireNotBlank(data.orderId(), "orderId");

        if (!Objects.equals(data.orderId(), messageKey)) {
            throw invalid("messageKey");
        }

        if (data.deliveryDate() == null) {
            throw invalid("deliveryDate");
        }

        if (!SUPPORTED_DELIVERY_SLOTS.contains(data.deliverySlot())) {
            throw invalid("deliverySlot");
        }

        if (data.lunchboxQuantity() == null
            || data.lunchboxQuantity() < 1) {
            throw invalid("lunchboxQuantity");
        }
    }

    private void validateRecipient(
        SubscriptionDeliveryOrderReadyEvent.Data data
    ) {
        requireNotBlank(data.recipientName(), "recipientName");
        requireNotBlank(data.recipientPhone(), "recipientPhone");
        requireNotBlank(data.postalCode(), "postalCode");
        requireNotBlank(data.addressLine1(), "addressLine1");
    }

    private void validateDeliveryMethod(
        SubscriptionDeliveryOrderReadyEvent.Data data
    ) {
        if (!SUPPORTED_DELIVERY_METHODS.contains(data.deliveryMethod())) {
            throw invalid("deliveryMethod");
        }

        if (OTHER_DELIVERY_METHOD.equals(data.deliveryMethod())) {
            requireNotBlank(
                data.deliveryMethodDetail()
                , "deliveryMethodDetail"
            );
        }
    }

    private void validateTerms(
        SubscriptionDeliveryOrderReadyEvent.Data data
    ) {
        if (!Boolean.TRUE.equals(data.termsAgreed())) {
            throw invalid("termsAgreed");
        }

        if (data.termsAgreedAt() == null) {
            throw invalid("termsAgreedAt");
        }
    }

    private void validateMenu(
        SubscriptionDeliveryOrderReadyEvent.Data data
    ) {
        if (data.menuItems() == null
            || data.menuItems().size() != 1) {
            throw invalid("menuItems");
        }

        SubscriptionDeliveryOrderReadyEvent.MenuItem menuItem =
            data.menuItems().getFirst();

        if (menuItem == null) {
            throw invalid("menuItems[0]");
        }

        requireNotBlank(menuItem.menuId(), "menuItems[0].menuId");
        requireNotBlank(menuItem.menuName(), "menuItems[0].menuName");

        if (menuItem.quantity() == null
            || menuItem.quantity() < 1) {
            throw invalid("menuItems[0].quantity");
        }

        if (!data.lunchboxQuantity().equals(menuItem.quantity())) {
            throw invalid("menuItems[0].quantity");
        }
    }

    private void validateAddressStructure(String addressLine1) {
        String normalizedAddress =
            addressLine1.trim().replaceAll("\\s+", " ");

        String[] addressParts = normalizedAddress.split(" ");

        if (addressParts.length < 2
            || addressParts[1].isBlank()) {
            throw invalid("addressLine1");
        }
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalid(fieldName);
        }
    }

    private void validateEventId(String eventId) {
        try {
            UUID.fromString(eventId);
        } catch (IllegalArgumentException exception) {
            throw invalid("eventId");
        }
    }

    private InvalidSubscriptionDeliveryOrderEventException invalid(
        String fieldName
    ) {
        return new InvalidSubscriptionDeliveryOrderEventException(
            "Invalid subscription delivery order event field: "
                + fieldName
        );
    }
}