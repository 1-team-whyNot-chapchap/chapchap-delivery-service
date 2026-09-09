package com.chapchap.delivery.domain.access.entity;

import com.chapchap.delivery.domain.access.constant.UserRole;
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
    name = "delivery_access_profiles"
    , uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_delivery_access_profiles_auth_user_id"
            , columnNames = "auth_user_id"
        )
    }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAccessProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_user_id", nullable = false)
    private Long authUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_role", nullable = false, length = 20)
    private UserRole lastRole;

    @Column(name = "access_allowed", nullable = false)
    private Boolean accessAllowed;

    @Column(name = "last_auth_event_occurred_at", nullable = false)
    private LocalDateTime lastAuthEventOccurredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DeliveryAccessProfile(
        Long authUserId
        , UserRole lastRole
        , Boolean accessAllowed
        , LocalDateTime lastAuthEventOccurredAt
    ) {
        this.authUserId = authUserId;
        this.lastRole = lastRole;
        this.accessAllowed = accessAllowed;
        this.lastAuthEventOccurredAt = lastAuthEventOccurredAt;
    }

    public void updateAuthState(
        UserRole lastRole
        , Boolean accessAllowed
        , LocalDateTime lastAuthEventOccurredAt
    ) {
        this.lastRole = lastRole;
        this.accessAllowed = accessAllowed;
        this.lastAuthEventOccurredAt = lastAuthEventOccurredAt;
    }
}