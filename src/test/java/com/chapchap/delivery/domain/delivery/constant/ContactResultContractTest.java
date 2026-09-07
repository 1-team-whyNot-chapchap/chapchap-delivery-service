package com.chapchap.delivery.domain.delivery.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContactResultContractTest {
    @Test
    @DisplayName("고객 연락 결과는 확정된 두 값만 제공한다")
    void exposesOnlyDocumentedValues() {
        assertThat(ContactResult.values())
            .containsExactly(ContactResult.CONTACTED, ContactResult.NO_ANSWER);
    }

    @Test
    @DisplayName("완료와 실패 연락 결과에 동일한 DB CHECK 제약을 적용한다")
    void migrationConstrainsPersistedValues() throws IOException {
        String migration = Files.readString(Path.of(
            "src/main/resources/db/migration/schema/"
                + "V1_22__add_contact_result_constraints.sql"
        ));

        assertThat(migration)
            .contains("chk_delivery_completions_contact_result")
            .contains("chk_delivery_failures_contact_result")
            .contains("contact_result IN ('CONTACTED', 'NO_ANSWER')");
    }
}
