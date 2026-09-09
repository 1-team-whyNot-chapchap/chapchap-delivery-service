CREATE TABLE deliveries (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_group_id BIGINT UNSIGNED NOT NULL
    , source_order_id VARCHAR(100) NOT NULL
    , delivery_public_id CHAR(36) NOT NULL
    , customer_id BIGINT UNSIGNED NOT NULL
    , delivery_area_code VARCHAR(50) NOT NULL
    , lunchbox_quantity INT NOT NULL
    , rotation_menu_id VARCHAR(100) NOT NULL
    , menu_name_snapshot VARCHAR(200) NOT NULL
    , request_handoff_type VARCHAR(10) NOT NULL
    , terms_agreed BOOLEAN NOT NULL
    , terms_agreed_at DATETIME NOT NULL
    , status VARCHAR(12) NOT NULL
    , delivery_version INT NOT NULL DEFAULT 1
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT uk_deliveries_source_order_id
        UNIQUE (source_order_id)
    , CONSTRAINT uk_deliveries_delivery_public_id
        UNIQUE (delivery_public_id)

    , CONSTRAINT chk_deliveries_lunchbox_quantity
        CHECK (lunchbox_quantity > 0)
    , CONSTRAINT chk_deliveries_request_handoff_type
        CHECK (
            request_handoff_type IN (
               'DIRECT'
                , 'DOORSTEP'
                , 'OTHER'
            )
        )
    , CONSTRAINT chk_deliveries_terms_agreed
        CHECK (terms_agreed = TRUE)
    , CONSTRAINT chk_deliveries_status
        CHECK (
            status IN (
                'READY'
                , 'DELIVERING'
                , 'DELIVERED'
                , 'FAILED'
            )
        )
    , CONSTRAINT chk_deliveries_delivery_version
        CHECK (delivery_version >= 1)
)
;