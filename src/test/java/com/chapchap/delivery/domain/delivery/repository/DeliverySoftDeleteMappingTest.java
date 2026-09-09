package com.chapchap.delivery.domain.delivery.repository;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.chapchap.delivery.domain.delivery.entity.Delivery;
import com.chapchap.delivery.domain.delivery.entity.DeliveryGroup;
import com.chapchap.delivery.domain.delivery.entity.DeliverySlot;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliverySoftDeleteMappingTest {
    @Test
    @DisplayName("Delivery soft-delete 필드와 현재 조회 HQL을 함께 매핑할 수 있다")
    void mapsDeletedAtAndParsesCurrentQuery() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.MySQLDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .build();

        try {
            assertThatCode(() -> {
                try (var sessionFactory = new MetadataSources(registry)
                    .addAnnotatedClass(Delivery.class)
                    .addAnnotatedClass(DeliveryGroup.class)
                    .addAnnotatedClass(DeliverySlot.class)
                    .buildMetadata()
                    .buildSessionFactory()) {
                    try (var session = sessionFactory.openSession()) {
                        session.createQuery(
                            "SELECT d FROM Delivery d WHERE d.deletedAt IS NULL"
                            , Delivery.class
                        );
                    }
                }
            }).doesNotThrowAnyException();
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
