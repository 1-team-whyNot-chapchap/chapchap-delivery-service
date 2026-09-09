package com.chapchap.delivery.domain.delivery.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_recipient_snapshots")
@SoftDelete(
    strategy = SoftDeleteType.TIMESTAMP
    , columnName = "deleted_at"
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRecipientSnapshot {
    @Id
    @Column(name = "delivery_id")
    private Long deliveryId;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @MapsId
    @JoinColumn(
        name = "delivery_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Delivery delivery;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "phone_encrypted", nullable = false, length = 256)
    private byte[] phoneEncrypted;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(name = "base_address", nullable = false)
    private String baseAddress;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "entrance_info_encrypted", length = 512)
    private byte[] entranceInfoEncrypted;

    @Column(name = "other_request", length = 500)
    private String otherRequest;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeliveryRecipientSnapshot(
        Delivery delivery
        , String recipientName
        , byte[] phoneEncrypted
        , String postalCode
        , String baseAddress
        , String detailAddress
        , byte[] entranceInfoEncrypted
        , String otherRequest
    ) {
        this.delivery = delivery;
        this.recipientName = recipientName;
        this.phoneEncrypted = phoneEncrypted;
        this.postalCode = postalCode;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
        this.entranceInfoEncrypted = entranceInfoEncrypted;
        this.otherRequest = otherRequest;
    }
}
