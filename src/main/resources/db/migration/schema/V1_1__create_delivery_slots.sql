CREATE TABLE delivery_slots (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , code VARCHAR(10) NOT NULL
    , start_time TIME NOT NULL
    , end_time TIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT uk_delivery_slots_code
        UNIQUE (code)
)
;