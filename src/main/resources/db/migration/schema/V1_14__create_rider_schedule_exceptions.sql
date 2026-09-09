CREATE TABLE rider_schedule_exceptions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , rider_id BIGINT UNSIGNED NOT NULL
    , schedule_date DATE NOT NULL
    , slot_id BIGINT UNSIGNED NOT NULL
    , is_working BOOLEAN NOT NULL
    , reason_code VARCHAR(32) NOT NULL
    , reason_detail VARCHAR(255) NULL DEFAULT NULL
    , version BIGINT UNSIGNED NOT NULL DEFAULT 0
    , created_by BIGINT UNSIGNED NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT uk_rider_schedule_exceptions_rider_date_slot
        UNIQUE (rider_id, schedule_date, slot_id)

    , CONSTRAINT chk_rider_schedule_exceptions_reason_code
        CHECK (
            reason_code IN (
                'ANNUAL_LEAVE'
                , 'SICK_LEAVE'
                , 'TRAINING'
                , 'SUBSTITUTE_WORK'
                , 'OTHER'
            )
        )
    , CONSTRAINT chk_rider_schedule_exceptions_other_reason_detail
        CHECK (
            reason_code <> 'OTHER'
                OR (
                        reason_detail IS NOT NULL
                    AND TRIM(reason_detail) <> ''
                )
        )
)
;