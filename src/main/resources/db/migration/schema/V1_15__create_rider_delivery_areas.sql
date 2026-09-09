CREATE TABLE rider_delivery_areas (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , rider_id BIGINT UNSIGNED NOT NULL
    , delivery_area_code VARCHAR(50) NOT NULL
    , effective_from DATE NOT NULL
    , effective_to DATE NULL DEFAULT NULL
    , is_active BOOLEAN NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT uk_rider_delivery_areas_rider_area_effective_from
        UNIQUE (rider_id, delivery_area_code, effective_from)

    , CONSTRAINT chk_rider_delivery_areas_effective_period
        CHECK (
                effective_to IS NULL
            OR effective_to >= effective_from
        )
)
;