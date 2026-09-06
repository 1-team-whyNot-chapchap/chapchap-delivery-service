package com.chapchap.delivery.domain.delivery.entity;

import com.chapchap.delivery.domain.delivery.constant.DeliveryAdminRecoveryReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryRecoveryResult;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "delivery_admin_recoveries")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAdminRecovery {
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
    @Column(name = "recovery_result", nullable = false, length = 10)
    private DeliveryRecoveryResult recoveryResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 32)
    private DeliveryAdminRecoveryReason reasonCode;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "actual_rider_id", nullable = false)
    private Long actualRiderId;

    @Column(name = "recovered_by", nullable = false)
    private Long recoveredBy;

    @Column(name = "recovered_at", nullable = false)
    private LocalDateTime recoveredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeliveryAdminRecovery(
        Delivery delivery
        , DeliveryRecoveryResult recoveryResult
        , DeliveryAdminRecoveryReason reasonCode
        , String reasonDetail
        , Long actualRiderId
        , Long recoveredBy
        , LocalDateTime recoveredAt
    ) {
        this.delivery = delivery;
        this.recoveryResult = recoveryResult;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.actualRiderId = actualRiderId;
        this.recoveredBy = recoveredBy;
        this.recoveredAt = recoveredAt;
    }
}
