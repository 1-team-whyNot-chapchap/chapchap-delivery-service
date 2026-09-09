CREATE TABLE delivery_assignment_issues (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , assignment_id BIGINT UNSIGNED NOT NULL
    , issue_code VARCHAR(32) NOT NULL
    , issue_detail VARCHAR(500) NULL DEFAULT NULL
    , reported_by BIGINT UNSIGNED NOT NULL
    , reported_at DATETIME NOT NULL
    , resolution VARCHAR(12) NULL DEFAULT NULL
    , resolved_by BIGINT UNSIGNED NULL DEFAULT NULL
    , resolved_at DATETIME NULL DEFAULT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , deleted_at DATETIME NULL DEFAULT NULL

    , PRIMARY KEY (id)

    , CONSTRAINT chk_delivery_assignment_issues_issue_code
        CHECK (
            issue_code IN (
                'SCHEDULE_CONFLICT'
                , 'CAPACITY_CONCERN'
                , 'AREA_DIFFICULTY'
                , 'VEHICLE_UNAVAILABLE'
                , 'HEALTH_ISSUE'
                , 'PERSONAL_EMERGENCY'
                , 'OTHER'
            )
        )

    , CONSTRAINT chk_delivery_assignment_issues_resolution
        CHECK (
            resolution IS NULL
                OR resolution IN ('REJECTED', 'REASSIGNED')
        )

    , CONSTRAINT chk_delivery_assignment_issues_other_detail
        CHECK (
            issue_code <> 'OTHER'
                OR issue_detail IS NOT NULL
        )

    , CONSTRAINT chk_delivery_assignment_issues_resolution_fields
        CHECK (
            (resolution IS NULL AND resolved_by IS NULL AND resolved_at IS NULL)
                OR (resolution IS NOT NULL AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL)
        )
)
;