package com.chapchap.delivery.domain.rider.entity;

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
    name = "riders"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_riders_auth_user_id"
            , columnNames = "auth_user_id"
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;

    @Column(name = "is_delivery_active", nullable = false)
    private Boolean isDeliveryActive;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Rider(Long authUserId) {
        this.authUserId = authUserId;
        this.isDeliveryActive = false;
    }

    public void changeDeliveryActive(boolean isDeliveryActive) {
        this.isDeliveryActive = isDeliveryActive;
    }
}