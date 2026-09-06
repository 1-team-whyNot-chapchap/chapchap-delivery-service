package com.chapchap.delivery.domain.delivery.entity;

import com.chapchap.delivery.domain.delivery.constant.DeliveryResultCorrectionReason;
import com.chapchap.delivery.domain.delivery.constant.DeliveryResultType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_result_corrections")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryResultCorrection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
        name = "delivery_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 20)
    private DeliveryResultType resultType;

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 32)
    private DeliveryResultCorrectionReason reasonCode;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "corrected_by", nullable = false)
    private Long correctedBy;

    @Column(name = "corrected_at", nullable = false)
    private LocalDateTime correctedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeliveryResultCorrection(
        Delivery delivery
        , DeliveryResultType resultType
        , String fieldName
        , String beforeValue
        , String afterValue
        , DeliveryResultCorrectionReason reasonCode
        , String reasonDetail
        , Long correctedBy
        , LocalDateTime correctedAt
    ) {
        this.delivery = delivery;
        this.resultType = resultType;
        this.fieldName = fieldName;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.reasonCode = reasonCode;
        this.reasonDetail = reasonDetail;
        this.correctedBy = correctedBy;
        this.correctedAt = correctedAt;
    }
}
