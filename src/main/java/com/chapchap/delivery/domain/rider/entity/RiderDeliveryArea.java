package com.chapchap.delivery.domain.rider.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "rider_delivery_areas"
    , uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_rider_delivery_areas_rider_area_effective_from"
        , columnNames = {
        "rider_id"
        , "delivery_area_code"
        , "effective_from"
    }
    )
}
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiderDeliveryArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "rider_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Rider rider;

    @Column(name = "delivery_area_code", nullable = false, length = 50)
    private String deliveryAreaCode;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public RiderDeliveryArea(
        Rider rider
        , String deliveryAreaCode
        , LocalDate effectiveFrom
        , LocalDate effectiveTo
        , Boolean isActive
    ) {
        this.rider = rider;
        this.deliveryAreaCode = deliveryAreaCode;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.isActive = isActive;
    }

    public void change(
        LocalDate effectiveTo
        , Boolean isActive
    ) {
        this.effectiveTo = effectiveTo;
        this.isActive = isActive;
    }

    public void restore(
        LocalDate effectiveTo
        , Boolean isActive
    ) {
        this.effectiveTo = effectiveTo;
        this.isActive = isActive;
        this.deletedAt = null;
    }
}