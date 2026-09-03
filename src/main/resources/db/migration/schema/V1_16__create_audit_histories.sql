CREATE TABLE audit_histories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , entity_type VARCHAR(50) NOT NULL
    , entity_id BIGINT UNSIGNED NOT NULL
    , action VARCHAR(50) NOT NULL
    , actor_id BIGINT UNSIGNED NULL DEFAULT NULL
    , actor_type VARCHAR(10) NOT NULL
    , reason_code VARCHAR(32) NULL DEFAULT NULL
    , reason_detail VARCHAR(500) NULL DEFAULT NULL
    , before_value_json JSON NULL
    , after_value_json JSON NULL
    , occurred_at DATETIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (id)

    , CONSTRAINT chk_audit_histories_actor_type
        CHECK (
            actor_type IN (
                'SYSTEM'
                , 'ADMIN'
                , 'RIDER'
            )
        )
    , CONSTRAINT chk_audit_histories_other_reason_detail
        CHECK (
            reason_code <> 'OTHER'
                OR (
                        reason_detail IS NOT NULL
                    AND TRIM(reason_detail) <> ''
                )
        )
)
;