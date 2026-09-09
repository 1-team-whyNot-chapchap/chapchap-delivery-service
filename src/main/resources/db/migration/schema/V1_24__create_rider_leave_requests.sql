CREATE TABLE rider_leave_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , rider_id BIGINT UNSIGNED NOT NULL
    , leave_date DATE NOT NULL
    , leave_slot VARCHAR(16) NOT NULL
    , leave_type VARCHAR(32) NOT NULL
    , reason_detail VARCHAR(255) NULL DEFAULT NULL
    , status VARCHAR(16) NOT NULL
    , requested_at DATETIME NOT NULL
    , reviewed_by BIGINT UNSIGNED NULL DEFAULT NULL
    , reviewed_at DATETIME NULL DEFAULT NULL
    , review_reason_detail VARCHAR(500) NULL DEFAULT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (id)
    , CONSTRAINT chk_rider_leave_requests_slot CHECK (leave_slot IN ('ALL_DAY', 'LUNCH', 'DINNER'))
    , CONSTRAINT chk_rider_leave_requests_type CHECK (leave_type IN ('ANNUAL_LEAVE', 'SICK_LEAVE', 'OTHER'))
    , CONSTRAINT chk_rider_leave_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
    , CONSTRAINT chk_rider_leave_requests_other_reason_detail CHECK (
        leave_type <> 'OTHER' OR (reason_detail IS NOT NULL AND TRIM(reason_detail) <> '')
    )
    , CONSTRAINT chk_rider_leave_requests_rejection_reason CHECK (
        status <> 'REJECTED' OR (review_reason_detail IS NOT NULL AND TRIM(review_reason_detail) <> '')
    )
)
;
