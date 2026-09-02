CREATE TABLE rider_weekly_schedules (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , rider_id BIGINT UNSIGNED NOT NULL
    , day_of_week TINYINT UNSIGNED NOT NULL
    , slot_id BIGINT UNSIGNED NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT uk_rider_weekly_schedules_rider_day_slot
        UNIQUE (rider_id, day_of_week, slot_id)

    , CONSTRAINT chk_rider_weekly_schedules_day_of_week
        CHECK (day_of_week BETWEEN 1 AND 7)
)
;