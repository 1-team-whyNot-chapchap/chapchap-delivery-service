CREATE TABLE delivery_result_corrections (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , delivery_id BIGINT UNSIGNED NOT NULL
    , result_type VARCHAR(20) NOT NULL
    , field_name VARCHAR(50) NOT NULL
    , before_value TEXT NULL
    , after_value TEXT NULL
    , reason_code VARCHAR(32) NOT NULL
    , reason_detail VARCHAR(500) NULL DEFAULT NULL
    , corrected_by BIGINT UNSIGNED NOT NULL
    , corrected_at DATETIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (id)

    , CONSTRAINT chk_delivery_result_corrections_result_type
        CHECK (
            result_type IN (
                'COMPLETION'
                , 'FAILURE'
            )
        )
    , CONSTRAINT chk_delivery_result_corrections_field_name
        CHECK (
            TRIM(field_name) <> ''
        )
    , CONSTRAINT chk_delivery_result_corrections_reason_code
        CHECK (
            reason_code IN (
                'DATA_ENTRY_ERROR'
                , 'CUSTOMER_REPORT'
                , 'OPERATIONAL_REVIEW'
                , 'OTHER'
            )
        )
    , CONSTRAINT chk_delivery_result_corrections_other_reason_detail
        CHECK (
            reason_code <> 'OTHER'
                OR (
                        reason_detail IS NOT NULL
                    AND TRIM(reason_detail) <> ''
                )
        )
)
;
