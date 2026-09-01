package com.chapchap.delivery.domain.delivery.entity;

import com.chapchap.delivery.domain.delivery.type.DeliveryGroupChangedByType;
import com.chapchap.delivery.domain.delivery.type.DeliveryGroupStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_group_status_histories")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryGroupStatusHistory {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 24)
    private DeliveryGroupStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 24)
    private DeliveryGroupStatus toStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_type", nullable = false, length = 10)
    private DeliveryGroupChangedByType changedByType;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeliveryGroupStatusHistory(
        DeliveryGroup deliveryGroup
        , DeliveryGroupStatus fromStatus
        , DeliveryGroupStatus toStatus
        , Long changedBy
        , DeliveryGroupChangedByType changedByType
        , LocalDateTime changedAt
    ) {
        this.deliveryGroup = deliveryGroup;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.changedByType = changedByType;
        this.changedAt = changedAt;
    }
}