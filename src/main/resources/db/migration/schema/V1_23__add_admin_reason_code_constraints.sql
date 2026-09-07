ALTER TABLE audit_histories
    ADD CONSTRAINT chk_audit_histories_reason_code
        CHECK (
            reason_code IS NULL
            OR reason_code IN (
                'INITIAL_ACTIVATION', 'TRAINING', 'LONG_TERM_LEAVE',
                'OPERATIONAL_HOLD', 'RESUME_DELIVERY',
                'ANNUAL_LEAVE', 'SICK_LEAVE', 'SUBSTITUTE_WORK',
                'AUTO_ASSIGNMENT_FAILED', 'LATE_ORDER', 'AREA_EXCEPTION',
                'OPERATIONAL_ADJUSTMENT',
                'RIDER_ISSUE', 'RIDER_UNAVAILABLE', 'ACKNOWLEDGEMENT_OVERDUE',
                'RIDER_ACCIDENT', 'RIDER_HEALTH_ISSUE', 'VEHICLE_ISSUE',
                'URGENT_OPERATIONAL_CHANGE',
                'RIDER_REPORT_CONFIRMED', 'CUSTOMER_REPORT_CONFIRMED',
                'OPERATIONAL_REVIEW', 'SYSTEM_RECOVERY',
                'DEVICE_FAILURE', 'NETWORK_FAILURE', 'APP_FAILURE',
                'SERVER_FAILURE',
                'DATA_ENTRY_ERROR', 'CUSTOMER_REPORT',
                'REJECTED', 'OTHER'
            )
        )
;

ALTER TABLE delivery_completions
    ADD CONSTRAINT chk_delivery_completions_admin_reason_value
        CHECK (
            admin_reason_code IS NULL
            OR admin_reason_code IN (
                'DEVICE_FAILURE', 'NETWORK_FAILURE', 'APP_FAILURE',
                'SERVER_FAILURE', 'OTHER'
            )
        )
    , ADD CONSTRAINT chk_delivery_completions_admin_other_detail
        CHECK (
            admin_reason_code <> 'OTHER'
            OR (admin_reason_detail IS NOT NULL AND TRIM(admin_reason_detail) <> '')
        )
    , ADD CONSTRAINT chk_delivery_completions_admin_reason_owner
        CHECK (
            processed_by_type = 'ADMIN'
            OR (admin_reason_code IS NULL AND admin_reason_detail IS NULL)
        )
;

ALTER TABLE delivery_failures
    ADD CONSTRAINT chk_delivery_failures_admin_reason_value
        CHECK (
            admin_reason_code IS NULL
            OR admin_reason_code IN (
                'RIDER_REPORT_CONFIRMED', 'CUSTOMER_REPORT_CONFIRMED',
                'OPERATIONAL_REVIEW', 'SYSTEM_RECOVERY',
                'OTHER'
            )
        )
    , ADD CONSTRAINT chk_delivery_failures_admin_other_detail
        CHECK (
            admin_reason_code <> 'OTHER'
            OR (admin_reason_detail IS NOT NULL AND TRIM(admin_reason_detail) <> '')
        )
    , ADD CONSTRAINT chk_delivery_failures_admin_reason_owner
        CHECK (
            processed_by_type = 'ADMIN'
            OR (admin_reason_code IS NULL AND admin_reason_detail IS NULL)
        )
;
