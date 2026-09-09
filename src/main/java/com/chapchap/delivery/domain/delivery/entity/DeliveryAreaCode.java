package com.chapchap.delivery.domain.delivery.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "delivery_area_codes"
    , uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_delivery_area_codes_area_code"
        , columnNames = "area_code"
    )
    , @UniqueConstraint(
    name = "uk_delivery_area_codes_district"
    , columnNames = "district"
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
public class DeliveryAreaCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_code", nullable = false, length = 50)
    private String areaCode;

    @Column(nullable = false, length = 20)
    private String district;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}