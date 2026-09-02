CREATE TABLE delivery_access_profiles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
    , auth_user_id BIGINT UNSIGNED NOT NULL
    , last_role VARCHAR(20) NOT NULL
    , access_allowed BOOLEAN NOT NULL
    , last_auth_event_occurred_at DATETIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (id)

    , CONSTRAINT uk_delivery_access_profiles_auth_user_id
        UNIQUE (auth_user_id)

    , CONSTRAINT chk_delivery_access_profiles_last_role
        CHECK (
            last_role IN (
                'CUSTOMER'
                , 'RIDER'
                , 'ADMIN'
            )
        )
)
;