CREATE TABLE delivery_recipient_snapshots (
    delivery_id BIGINT UNSIGNED NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    phone_encrypted VARBINARY(256) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    base_address VARCHAR(255) NOT NULL,
    detail_address VARCHAR(255) NULL,
    entrance_info_encrypted VARBINARY(512) NULL,
    other_request VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    deleted_at DATETIME NULL DEFAULT NULL,

    PRIMARY KEY (delivery_id)
)
;