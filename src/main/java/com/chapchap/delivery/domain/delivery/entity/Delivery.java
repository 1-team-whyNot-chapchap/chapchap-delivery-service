package com.chapchap.delivery.domain.delivery.entity;

import com.chapchap.delivery.domain.delivery.type.DeliveryStatus;
import com.chapchap.delivery.domain.delivery.type.RequestHandoffType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "deliveries"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_deliveries_source_order_id"
            , columnNames = "source_order_id"
        )
        , @UniqueConstraint(
            name = "uk_deliveries_delivery_public_id"
            , columnNames = "delivery_public_id"
        )
    }
)
@SoftDelete(
    strategy = SoftDeleteType.TIMESTAMP
    , columnName = "deleted_at"
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "delivery_group_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private DeliveryGroup deliveryGroup;

    @Column(name = "source_order_id", nullable = false, length = 100)
    private String sourceOrderId;

    @Column(name = "delivery_public_id", nullable = false, length = 36)
    private String deliveryPublicId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "delivery_area_code", nullable = false, length = 50)
    private String deliveryAreaCode;

    @Column(name = "lunchbox_quantity", nullable = false)
    private Integer lunchboxQuantity;

    @Column(name = "rotation_menu_id", nullable = false, length = 100)
    private String rotationMenuId;

    @Column(name = "menu_name_snapshot", nullable = false, length = 200)
    private String menuNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_handoff_type", nullable = false, length = 10)
    private RequestHandoffType requestHandoffType;

    @Column(name = "terms_agreed", nullable = false)
    private Boolean termsAgreed;

    @Column(name = "terms_agreed_at", nullable = false)
    private LocalDateTime termsAgreedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DeliveryStatus status;

    @Column(name = "delivery_version", nullable = false)
    private Integer deliveryVersion;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Delivery(
        DeliveryGroup deliveryGroup
        , String sourceOrderId
        , String deliveryPublicId
        , Long customerId
        , String deliveryAreaCode
        , Integer lunchboxQuantity
        , String rotationMenuId
        , String menuNameSnapshot
        , RequestHandoffType requestHandoffType
        , LocalDateTime termsAgreedAt
    ) {
        this.deliveryGroup = deliveryGroup;
        this.sourceOrderId = sourceOrderId;
        this.deliveryPublicId = deliveryPublicId;
        this.customerId = customerId;
        this.deliveryAreaCode = deliveryAreaCode;
        this.lunchboxQuantity = lunchboxQuantity;
        this.rotationMenuId = rotationMenuId;
        this.menuNameSnapshot = menuNameSnapshot;
        this.requestHandoffType = requestHandoffType;
        this.termsAgreed = true;
        this.termsAgreedAt = termsAgreedAt;

        this.status = DeliveryStatus.READY;
        this.deliveryVersion = 1;
    }
}
