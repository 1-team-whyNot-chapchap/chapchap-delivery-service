CREATE TABLE delivery_assignments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_group_id BIGINT UNSIGNED NOT NULL
    , rider_id BIGINT UNSIGNED NOT NULL
    , assignment_type VARCHAR(10) NOT NULL
    , status VARCHAR(16) NOT NULL
    , assigned_at DATETIME NOT NULL
    , notified_at DATETIME NULL DEFAULT NULL
    , acknowledged_at DATETIME NULL DEFAULT NULL
    , confirmed_by BIGINT UNSIGNED NULL DEFAULT NULL
    , confirmed_at DATETIME NULL DEFAULT NULL
    , created_by BIGINT UNSIGNED NULL DEFAULT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT chk_delivery_assignments_assignment_type
        CHECK (assignment_type IN ('AUTO', 'MANUAL'))

    , CONSTRAINT chk_delivery_assignments_status
        CHECK (status IN ('ASSIGNED', 'ACKNOWLEDGED', 'ISSUE_REPORTED', 'CONFIRMED', 'REASSIGNED'))
)
;