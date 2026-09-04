CREATE TABLE delivery_assignment_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , assignment_id BIGINT UNSIGNED NOT NULL
    , delivery_id BIGINT UNSIGNED NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT uk_delivery_assignment_items_assignment_delivery
        UNIQUE (assignment_id, delivery_id)
)
;