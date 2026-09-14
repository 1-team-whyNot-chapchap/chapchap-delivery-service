CREATE TABLE rider_current_locations (
    rider_id BIGINT UNSIGNED NOT NULL
    , latitude DECIMAL(10,7) NOT NULL
    , longitude DECIMAL(11,7) NOT NULL
    , accuracy_m DECIMAL(8,2) NOT NULL
    , captured_at DATETIME NOT NULL
    , received_at DATETIME NOT NULL
    , created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()
    , updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP()

    , PRIMARY KEY (rider_id)
)
;
