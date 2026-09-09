package com.chapchap.delivery.domain.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminReasonMigrationContractTest {
    @Test
    @DisplayName("관리자 사유 Migration은 범용 감사와 완료·실패 원본을 CHECK로 제한한다")
    void migrationContainsReasonChecks() throws IOException {
        String migration;
        try (var stream = getClass().getResourceAsStream(
            "/db/migration/schema/V1_23__add_admin_reason_code_constraints.sql"
        )) {
            assertThat(stream).isNotNull();
            migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(migration)
            .contains("chk_audit_histories_reason_code")
            .contains("chk_delivery_completions_admin_reason_value")
            .contains("chk_delivery_failures_admin_reason_value")
            .contains("chk_delivery_completions_admin_other_detail")
            .contains("chk_delivery_failures_admin_other_detail")
            .contains("chk_delivery_completions_admin_reason_owner")
            .contains("chk_delivery_failures_admin_reason_owner")
            .contains("'AREA_EXCEPTION'")
            .contains("'ACKNOWLEDGEMENT_OVERDUE'")
            .contains("'URGENT_OPERATIONAL_CHANGE'")
            .contains("'SYSTEM_RECOVERY'")
            .contains("'DEVICE_FAILURE'");
    }
}
