package com.chapchap.delivery.domain.riderlocation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Current-only rider position. This table deliberately has no location history. */
@Entity
@Table(name = "rider_current_locations")
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiderCurrentLocation {
    @Id
    @Column(name = "rider_id")
    private Long riderId;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 7)
    private BigDecimal longitude;

    @Column(name = "accuracy_m", nullable = false, precision = 8, scale = 2)
    private BigDecimal accuracyM;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RiderCurrentLocation(
        Long riderId, BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyM,
        LocalDateTime capturedAt, LocalDateTime receivedAt
    ) {
        this.riderId = riderId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyM = accuracyM;
        this.capturedAt = capturedAt;
        this.receivedAt = receivedAt;
    }

    public void replaceWithNewMeasurement(
        BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyM,
        LocalDateTime capturedAt, LocalDateTime receivedAt
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyM = accuracyM;
        this.capturedAt = capturedAt;
        this.receivedAt = receivedAt;
    }

    public void refreshReceipt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
