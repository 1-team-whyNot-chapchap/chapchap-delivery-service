CREATE TABLE delivery_completions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_id BIGINT UNSIGNED NOT NULL
    , actual_handoff_type VARCHAR(10) NOT NULL
    , storage_location VARCHAR(100) NULL DEFAULT NULL
    , contact_attempted_at DATETIME NULL DEFAULT NULL
    , contact_result VARCHAR(30) NULL DEFAULT NULL
    , processed_by BIGINT UNSIGNED NOT NULL
    , processed_by_type VARCHAR(10) NOT NULL
    , admin_reason_code VARCHAR(32) NULL DEFAULT NULL
    , admin_reason_detail VARCHAR(500) NULL DEFAULT NULL
    , completed_at DATETIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (id)

    , CONSTRAINT uk_delivery_completions_delivery_id
        UNIQUE (delivery_id)

    , CONSTRAINT chk_delivery_completions_actual_handoff_type
        CHECK (
            actual_handoff_type IN (
                'DIRECT'
                , 'DOORSTEP'
                , 'OTHER'
            )
        )
    , CONSTRAINT chk_delivery_completions_processed_by_type
        CHECK (
            processed_by_type IN (
                'RIDER'
                , 'ADMIN'
            )
        )
    , CONSTRAINT chk_delivery_completions_storage_location
        CHECK (
            actual_handoff_type = 'DIRECT'
                OR (
                        storage_location IS NOT NULL
                    AND TRIM(storage_location) <> ''
                )
        )
    , CONSTRAINT chk_delivery_completions_admin_reason_code
        CHECK (
                processed_by_type <> 'ADMIN'
            OR admin_reason_code IS NOT NULL
        )
)
;


CREATE TABLE delivery_completion_photos (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_completion_id BIGINT UNSIGNED NOT NULL
    , storage_key VARCHAR(500) NOT NULL
    , original_filename VARCHAR(255) NULL DEFAULT NULL
    , content_type VARCHAR(100) NOT NULL
    , file_size BIGINT UNSIGNED NOT NULL
    , uploaded_by BIGINT UNSIGNED NOT NULL
    , uploaded_at DATETIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT uk_delivery_completion_photos_completion_id
        UNIQUE (delivery_completion_id)
    , CONSTRAINT uk_delivery_completion_photos_storage_key
        UNIQUE (storage_key)

    , CONSTRAINT chk_delivery_completion_photos_file_size
        CHECK (file_size > 0)
)
;


CREATE TABLE delivery_failures (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_id BIGINT UNSIGNED NOT NULL
    , failure_stage VARCHAR(20) NOT NULL
    , failure_code VARCHAR(32) NOT NULL
    , failure_detail VARCHAR(500) NULL DEFAULT NULL
    , contact_attempted_at DATETIME NULL DEFAULT NULL
    , contact_result VARCHAR(30) NULL DEFAULT NULL
    , item_recovered BOOLEAN NOT NULL
    , recovered_at DATETIME NULL DEFAULT NULL
    , processed_by BIGINT UNSIGNED NOT NULL
    , processed_by_type VARCHAR(10) NOT NULL
    , admin_reason_code VARCHAR(32) NULL DEFAULT NULL
    , admin_reason_detail VARCHAR(500) NULL DEFAULT NULL
    , failed_at DATETIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (id)

    , CONSTRAINT uk_delivery_failures_delivery_id
        UNIQUE (delivery_id)

    , CONSTRAINT chk_delivery_failures_failure_stage
        CHECK (
            failure_stage IN (
                'BEFORE_DEPARTURE'
                , 'DURING_DELIVERY'
            )
        )
    , CONSTRAINT chk_delivery_failures_failure_code
        CHECK (
            failure_code IN (
                'CUSTOMER_UNAVAILABLE'
                , 'ACCESS_DENIED'
                , 'INVALID_ADDRESS'
                , 'CUSTOMER_REFUSED'
                , 'RIDER_UNAVAILABLE'
                , 'RIDER_ACCIDENT'
                , 'VEHICLE_ISSUE'
                , 'DELIVERY_OMITTED'
                , 'ITEM_MISSING'
                , 'ITEM_DAMAGED'
                , 'WEATHER_CONDITION'
                , 'ROAD_RESTRICTION'
                , 'EMERGENCY'
                , 'OTHER'
            )
        )
    , CONSTRAINT chk_delivery_failures_other_failure_detail
        CHECK (
            failure_code <> 'OTHER'
                OR (
                        failure_detail IS NOT NULL
                    AND TRIM(failure_detail) <> ''
                )
        )
    , CONSTRAINT chk_delivery_failures_processed_by_type
        CHECK (
            processed_by_type IN (
                'RIDER'
                , 'ADMIN'
            )
        )
    , CONSTRAINT chk_delivery_failures_recovered_at
        CHECK (
                item_recovered = FALSE
            OR recovered_at IS NOT NULL
        )
    , CONSTRAINT chk_delivery_failures_admin_reason_code
        CHECK (
                processed_by_type <> 'ADMIN'
            OR admin_reason_code IS NOT NULL
        )
)
;


CREATE TABLE delivery_delays (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_id BIGINT UNSIGNED NOT NULL
    , reason_code VARCHAR(32) NULL DEFAULT NULL
    , reason_detail VARCHAR(500) NULL DEFAULT NULL
    , detected_at DATETIME NOT NULL
    , delay_minutes INT NULL DEFAULT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (id)

    , CONSTRAINT uk_delivery_delays_delivery_id
        UNIQUE (delivery_id)

    , CONSTRAINT chk_delivery_delays_delay_minutes
        CHECK (
                delay_minutes IS NULL
            OR delay_minutes >= 1
        )
)
;

CREATE TABLE delivery_admin_recoveries (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_id BIGINT UNSIGNED NOT NULL
    , recovery_result VARCHAR(10) NOT NULL
    , reason_code VARCHAR(32) NOT NULL
    , reason_detail VARCHAR(500) NULL DEFAULT NULL
    , actual_rider_id BIGINT UNSIGNED NOT NULL
    , recovered_by BIGINT UNSIGNED NOT NULL
    , recovered_at DATETIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , PRIMARY KEY (id)
    , CONSTRAINT chk_delivery_admin_recoveries_result
        CHECK (recovery_result IN ('DELIVERED', 'FAILED'))
    , CONSTRAINT chk_delivery_admin_recoveries_reason
        CHECK (reason_code IN (
            'DEVICE_FAILURE', 'NETWORK_FAILURE', 'APP_FAILURE',
            'SERVER_FAILURE', 'OTHER'
        ))
    , CONSTRAINT chk_delivery_admin_recoveries_other_detail
        CHECK (
            reason_code <> 'OTHER'
                OR (reason_detail IS NOT NULL AND TRIM(reason_detail) <> '')
        )
)
;
