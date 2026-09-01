CREATE TABLE delivery_area_codes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , area_code VARCHAR(50) NOT NULL
    , district VARCHAR(20) NOT NULL
    , is_active BOOLEAN NOT NULL
    , deactivated_at DATETIME NULL DEFAULT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT uk_delivery_area_codes_area_code
        UNIQUE (area_code)
    , CONSTRAINT uk_delivery_area_codes_district
        UNIQUE (district)
)
;