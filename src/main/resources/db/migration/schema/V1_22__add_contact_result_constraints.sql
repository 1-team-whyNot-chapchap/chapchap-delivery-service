ALTER TABLE delivery_completions
    ADD CONSTRAINT chk_delivery_completions_contact_result
        CHECK (
            contact_result IS NULL
                OR contact_result IN ('CONTACTED', 'NO_ANSWER')
        )
;

ALTER TABLE delivery_failures
    ADD CONSTRAINT chk_delivery_failures_contact_result
        CHECK (
            contact_result IS NULL
                OR contact_result IN ('CONTACTED', 'NO_ANSWER')
        )
;
