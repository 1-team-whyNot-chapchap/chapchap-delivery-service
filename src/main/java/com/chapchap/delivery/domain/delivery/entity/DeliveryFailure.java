package com.chapchap.delivery.domain.delivery.entity;

import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureCode;
import com.chapchap.delivery.domain.delivery.constant.DeliveryFailureStage;
import com.chapchap.delivery.domain.delivery.constant.DeliveryProcessedByType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "delivery_failures"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_delivery_failures_delivery_id"
            , columnNames = "delivery_id"
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryFailure {
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

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_stage", nullable = false, length = 20)
    private DeliveryFailureStage failureStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", nullable = false, length = 32)
    private DeliveryFailureCode failureCode;

    @Column(name = "failure_detail", length = 500)
    private String failureDetail;

    @Column(name = "contact_attempted_at")
    private LocalDateTime contactAttemptedAt;

    @Column(name = "contact_result", length = 30)
    private String contactResult;

    @Column(name = "item_recovered", nullable = false)
    private Boolean itemRecovered;

    @Column(name = "recovered_at")
    private LocalDateTime recoveredAt;

    @Column(name = "processed_by", nullable = false)
    private Long processedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "processed_by_type", nullable = false, length = 10)
    private DeliveryProcessedByType processedByType;

    @Column(name = "admin_reason_code", length = 32)
    private String adminReasonCode;

    @Column(name = "admin_reason_detail", length = 500)
    private String adminReasonDetail;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeliveryFailure(
        Delivery delivery
        , DeliveryFailureStage failureStage
        , DeliveryFailureCode failureCode
        , String failureDetail
        , LocalDateTime contactAttemptedAt
        , String contactResult
        , Boolean itemRecovered
        , LocalDateTime recoveredAt
        , Long processedBy
        , LocalDateTime failedAt
    ) {
        this(
            delivery
            , failureStage
            , failureCode
            , failureDetail
            , contactAttemptedAt
            , contactResult
            , itemRecovered
            , recoveredAt
            , processedBy
            , DeliveryProcessedByType.RIDER
            , null
            , null
            , failedAt
        );
    }

    public DeliveryFailure(
        Delivery delivery
        , DeliveryFailureStage failureStage
        , DeliveryFailureCode failureCode
        , String failureDetail
        , LocalDateTime contactAttemptedAt
        , String contactResult
        , Boolean itemRecovered
        , LocalDateTime recoveredAt
        , Long processedBy
        , DeliveryProcessedByType processedByType
        , String adminReasonCode
        , String adminReasonDetail
        , LocalDateTime failedAt
    ) {
        this.delivery = delivery;
        this.failureStage = failureStage;
        this.failureCode = failureCode;
        this.failureDetail = failureDetail;
        this.contactAttemptedAt = contactAttemptedAt;
        this.contactResult = contactResult;
        this.itemRecovered = itemRecovered;
        this.recoveredAt = recoveredAt;
        this.processedBy = processedBy;
        this.processedByType = processedByType;
        this.adminReasonCode = adminReasonCode;
        this.adminReasonDetail = adminReasonDetail;
        this.failedAt = failedAt;
    }
}
