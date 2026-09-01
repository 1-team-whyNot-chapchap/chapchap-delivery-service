package com.chapchap.delivery.domain.delivery.entity;

import com.chapchap.delivery.domain.delivery.type.DeliveryChangedByType;
import com.chapchap.delivery.domain.delivery.type.DeliveryStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_status_histories")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "delivery_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 12)
    private DeliveryStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 12)
    private DeliveryStatus toStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_type", nullable = false, length = 10)
    private DeliveryChangedByType changedByType;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeliveryStatusHistory(
        Delivery delivery
        , DeliveryStatus fromStatus
        , DeliveryStatus toStatus
        , Long changedBy
        , DeliveryChangedByType changedByType
        , LocalDateTime changedAt
    ) {
        this.delivery = delivery;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.changedByType = changedByType;
        this.changedAt = changedAt;
    }
}