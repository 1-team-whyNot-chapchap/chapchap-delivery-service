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
@Table(
    name = "delivery_completion_photos"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_delivery_completion_photos_completion_id"
            , columnNames = "delivery_completion_id"
        )
        , @UniqueConstraint(
            name = "uk_delivery_completion_photos_storage_key"
            , columnNames = "storage_key"
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
public class DeliveryCompletionPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "delivery_completion_id"
        , nullable = false
        , foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private DeliveryCompletion deliveryCompletion;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeliveryCompletionPhoto(
        DeliveryCompletion deliveryCompletion
        , String storageKey
        , String originalFilename
        , String contentType
        , Long fileSize
        , Long uploadedBy
        , LocalDateTime uploadedAt
    ) {
        this.deliveryCompletion = deliveryCompletion;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }
}