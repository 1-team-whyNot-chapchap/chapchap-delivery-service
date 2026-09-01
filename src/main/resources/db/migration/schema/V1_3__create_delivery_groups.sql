CREATE TABLE delivery_groups (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_date DATE NOT NULL
    , slot_id BIGINT UNSIGNED NOT NULL
    , status VARCHAR(24) NOT NULL
    , auto_assignment_completed_at DATETIME NULL  DEFAULT NULL
    , actual_started_at DATETIME NULL  DEFAULT NULL
    , actual_finished_at DATETIME NULL  DEFAULT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)
    , CONSTRAINT uk_delivery_groups_date_slot
         UNIQUE (delivery_date, slot_id)

    , CONSTRAINT chk_delivery_groups_status
        CHECK (
            status IN (
                'WAITING_ASSIGNMENT'
                , 'WAITING_RIDER'
                , 'ISSUE_REVIEW'
                , 'READY_TO_CONFIRM'
                , 'CONFIRMED'
                , 'IN_PROGRESS'
                , 'COMPLETED'
                , 'COMPLETED_WITH_FAILURE'
                , 'FAILED'
            )
        )
)
;