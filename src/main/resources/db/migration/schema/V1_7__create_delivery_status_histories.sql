CREATE TABLE delivery_status_histories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_id BIGINT UNSIGNED NOT NULL
    , from_status VARCHAR(12) NULL
    , to_status VARCHAR(12) NOT NULL
    , changed_by BIGINT UNSIGNED NULL
    , changed_by_type VARCHAR(10) NOT NULL
    , changed_at DATETIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (id)

    , CONSTRAINT chk_delivery_status_histories_from_status
        CHECK (
            from_status IS NULL
            OR from_status IN (
                'READY'
                , 'DELIVERING'
                , 'DELIVERED'
                , 'FAILED'
            )
        )
    , CONSTRAINT chk_delivery_status_histories_to_status
        CHECK (
            to_status IN (
                'READY'
                , 'DELIVERING'
                , 'DELIVERED'
                , 'FAILED'
            )
        )
    , CONSTRAINT chk_delivery_status_histories_changed_by_type
        CHECK (
            changed_by_type IN (
                'SYSTEM'
                , 'ADMIN'
                , 'RIDER'
            )
        )
)
;