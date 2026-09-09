package com.chapchap.delivery.domain.delivery.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "delivery_delays"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_delivery_delays_delivery_id"
            , columnNames = "delivery_id"
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryDelay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
        name = "delivery_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Delivery delivery;

    @Column(name = "reason_code", length = 32)
    private String reasonCode;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "delay_minutes")
    private Integer delayMinutes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DeliveryDelay(
        Delivery delivery
        , LocalDateTime detectedAt
    ) {
        this.delivery = delivery;
        this.detectedAt = detectedAt;
    }

    public DeliveryDelay(
        Delivery delivery
        , String reasonCode
        , String reasonDetail
        , LocalDateTime detectedAt
    ) {
        this.delivery = delivery;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.detectedAt = detectedAt;
    }

    public void finalizeDelay(
        Integer delayMinutes
    ) {
        if (delayMinutes == null || delayMinutes < 1) {
            throw new IllegalArgumentException(
                "Delay minutes must be greater than or equal to 1."
            );
        }

        this.delayMinutes = delayMinutes;
    }
}
