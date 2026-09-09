CREATE TABLE integration_event_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , event_id VARCHAR(100) NOT NULL
    , direction VARCHAR(10) NOT NULL
    , event_type VARCHAR(100) NOT NULL
    , aggregate_type VARCHAR(50) NULL
    , aggregate_id VARCHAR(100) NULL
    , business_key VARCHAR(200) NULL
    , status VARCHAR(10) NOT NULL
    , topic VARCHAR(200) NULL
    , event_key VARCHAR(200) NULL
    , payload_json JSON NULL
    , attempt_count INT NOT NULL DEFAULT 0
    , last_attempted_at DATETIME NULL
    , occurred_at DATETIME NOT NULL
    , processed_at DATETIME NULL
    , error_code VARCHAR(100) NULL
    , error_message VARCHAR(500) NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (id)
    
    , CONSTRAINT uk_integration_event_records_event_id
        UNIQUE (event_id)
    , CONSTRAINT uk_integration_event_records_business_key
        UNIQUE (business_key)

    , CONSTRAINT chk_integration_event_records_direction
        CHECK (
            direction IN (
                'CONSUME'
                , 'PUBLISH'
            )
        )
    , CONSTRAINT chk_integration_event_records_status
        CHECK (
            status IN (
                'SUCCESS'
                , 'FAILED'
                , 'IGNORED'
            )
        )
    , CONSTRAINT chk_integration_event_records_attempt_count
        CHECK (attempt_count >= 0)
)
;