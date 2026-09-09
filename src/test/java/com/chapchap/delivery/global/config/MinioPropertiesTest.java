package com.chapchap.delivery.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MinioPropertiesTest {
    @Test
    @DisplayName("설정된 이미지 경로와 파일 식별자를 MinIO 객체 키로 결합한다")
    void resolvesImageObjectKey() {
        MinioProperties properties = new MinioProperties(
            "http://localhost:9000"
            , "msa4-team1"
            , "access"
            , "secret"
            , "/delivery/completion-photos/"
            , Set.of("image/jpeg")
        );

        assertThat(properties.resolveImageObjectKey("public-id/photo-id"))
            .isEqualTo("delivery/completion-photos/public-id/photo-id");
    }
}
