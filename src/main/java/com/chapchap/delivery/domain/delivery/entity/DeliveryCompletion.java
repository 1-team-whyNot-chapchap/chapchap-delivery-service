package com.chapchap.delivery.domain.delivery.entity;

import com.chapchap.delivery.domain.delivery.constant.ActualHandoffType;
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
    name = "delivery_completions"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_delivery_completions_delivery_id"
            , columnNames = "delivery_id"
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryCompletion {
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
    @Column(name = "actual_handoff_type", nullable = false, length = 10)
    private ActualHandoffType actualHandoffType;

    @Column(name = "storage_location", length = 100)
    private String storageLocation;

    @Column(name = "contact_attempted_at")
    private LocalDateTime contactAttemptedAt;

    @Column(name = "contact_result", length = 30)
    private String contactResult;

    @Column(name = "processed_by", nullable = false)
    private Long processedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "processed_by_type", nullable = false, length = 10)
    private DeliveryProcessedByType processedByType;

    @Column(name = "admin_reason_code", length = 32)
    private String adminReasonCode;

    @Column(name = "admin_reason_detail", length = 500)
    private String adminReasonDetail;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeliveryCompletion(
        Delivery delivery
        , ActualHandoffType actualHandoffType
        , String storageLocation
        , LocalDateTime contactAttemptedAt
        , String contactResult
        , Long processedBy
        , LocalDateTime completedAt
    ) {
        this(
            delivery
            , actualHandoffType
            , storageLocation
            , contactAttemptedAt
            , contactResult
            , processedBy
            , DeliveryProcessedByType.RIDER
            , null
            , null
            , completedAt
        );
    }

    public DeliveryCompletion(
        Delivery delivery
        , ActualHandoffType actualHandoffType
        , String storageLocation
        , LocalDateTime contactAttemptedAt
        , String contactResult
        , Long processedBy
        , DeliveryProcessedByType processedByType
        , String adminReasonCode
        , String adminReasonDetail
        , LocalDateTime completedAt
    ) {
        this.delivery = delivery;
        this.actualHandoffType = actualHandoffType;
        this.storageLocation = storageLocation;
        this.contactAttemptedAt = contactAttemptedAt;
        this.contactResult = contactResult;
        this.processedBy = processedBy;
        this.processedByType = processedByType;
        this.adminReasonCode = adminReasonCode;
        this.adminReasonDetail = adminReasonDetail;
        this.completedAt = completedAt;
    }
}
