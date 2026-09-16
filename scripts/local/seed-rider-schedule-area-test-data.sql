-- Local-only seed data for the admin rider schedule and delivery-area screens.
-- Run manually against the local Delivery database. Do not register this file as a Flyway migration.
-- The test rider is reused on subsequent runs; no production data is deleted.

START TRANSACTION;

SET @test_rider_auth_user_id = 990001;

INSERT INTO riders (
    auth_user_id
    , is_delivery_active
    , version
    , deleted_at
) VALUES (
    @test_rider_auth_user_id
    , TRUE
    , 0
    , NULL
)
ON DUPLICATE KEY UPDATE
    is_delivery_active = TRUE
    , deleted_at = NULL
    , updated_at = CURRENT_TIMESTAMP();

SET @test_rider_id = (
    SELECT id
    FROM riders
    WHERE auth_user_id = @test_rider_auth_user_id
);

INSERT INTO rider_weekly_schedules (
    rider_id
    , day_of_week
    , slot_id
) SELECT
    @test_rider_id
    , 1
    , id
FROM delivery_slots
WHERE code = 'LUNCH'
ON DUPLICATE KEY UPDATE
    deleted_at = NULL;

INSERT INTO rider_weekly_schedules (
    rider_id
    , day_of_week
    , slot_id
) SELECT
    @test_rider_id
    , 3
    , id
FROM delivery_slots
WHERE code = 'DINNER'
ON DUPLICATE KEY UPDATE
    deleted_at = NULL;

INSERT INTO rider_delivery_areas (
    rider_id
    , delivery_area_code
    , effective_from
    , effective_to
    , is_active
    , deleted_at
) VALUES
    (@test_rider_id, 'DAEGU_JUNG_GU', '2026-01-01', NULL, TRUE, NULL)
    , (@test_rider_id, 'DAEGU_DONG_GU', '2026-01-01', NULL, TRUE, NULL)
ON DUPLICATE KEY UPDATE
    effective_to = NULL
    , is_active = TRUE
    , deleted_at = NULL
    , updated_at = CURRENT_TIMESTAMP();

COMMIT;

SELECT
    id AS rider_id
    , auth_user_id
    , is_delivery_active
FROM riders
WHERE id = @test_rider_id;

SELECT
    weekly_schedule.id AS weekly_schedule_id
    , weekly_schedule.day_of_week
    , delivery_slot.code AS delivery_slot
FROM rider_weekly_schedules weekly_schedule
JOIN delivery_slots delivery_slot
    ON delivery_slot.id = weekly_schedule.slot_id
WHERE weekly_schedule.rider_id = @test_rider_id
    AND weekly_schedule.deleted_at IS NULL
ORDER BY weekly_schedule.day_of_week, delivery_slot.code;

SELECT
    id AS rider_delivery_area_id
    , delivery_area_code
    , effective_from
    , effective_to
    , is_active
FROM rider_delivery_areas
WHERE rider_id = @test_rider_id
    AND deleted_at IS NULL
ORDER BY delivery_area_code;
