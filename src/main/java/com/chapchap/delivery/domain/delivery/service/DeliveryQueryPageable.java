package com.chapchap.delivery.domain.delivery.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class DeliveryQueryPageable {
    private static final int MAX_PAGE_SIZE = 100;

    private DeliveryQueryPageable() {
    }

    static Pageable customer(Pageable pageable) {
        return normalize(
            pageable
            , Map.of(
                "deliveryDate", "deliveryGroup.deliveryDate"
                , "deliverySlot", "deliveryGroup.slot.code"
                , "status", "status"
            )
            , Sort.Order.desc("deliveryGroup.deliveryDate")
        );
    }

    static Pageable adminGroup(Pageable pageable) {
        return normalize(
            pageable
            , Map.of(
                "deliveryDate", "deliveryDate"
                , "deliverySlot", "slot.code"
                , "status", "status"
            )
            , Sort.Order.desc("deliveryDate")
        );
    }

    private static Pageable normalize(
        Pageable pageable
        , Map<String, String> allowedProperties
        , Sort.Order defaultOrder
    ) {
        List<Sort.Order> orders = new ArrayList<>();
        pageable.getSort().forEach(order -> {
            String property = allowedProperties.get(order.getProperty());
            if (property != null) {
                orders.add(new Sort.Order(order.getDirection(), property));
            }
        });
        if (orders.isEmpty()) {
            orders.add(defaultOrder);
        }
        orders.add(Sort.Order.asc("id"));

        return PageRequest.of(
            pageable.getPageNumber()
            , Math.min(pageable.getPageSize(), MAX_PAGE_SIZE)
            , Sort.by(orders)
        );
    }
}
