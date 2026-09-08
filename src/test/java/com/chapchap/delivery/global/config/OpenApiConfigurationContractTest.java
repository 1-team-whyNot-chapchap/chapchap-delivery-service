package com.chapchap.delivery.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenApiConfigurationContractTest {

    @Test
    @DisplayName("OpenAPI 제목은 영어로, 서비스 설명은 한글로 표시한다")
    void openApiUsesEnglishTitleAndKoreanDescription() {
        OpenAPI openApi = new OpenApiConfig().deliveryOpenApi();

        assertThat(openApi.getInfo().getTitle())
            .isEqualTo("Chapchap Delivery Service API");
        assertThat(openApi.getInfo().getDescription())
            .contains("도시락 정기 구독 배송")
            .contains("기사 배송 수행")
            .contains("관리자 배정·운영");
        assertThat(openApi.getInfo().getVersion()).isNotBlank();
    }

    @Test
    @DisplayName("Swagger UI와 OpenAPI JSON 경로를 명시적으로 활성화한다")
    void applicationEnablesSwaggerUiAndApiDocs() throws IOException {
        String application = Files.readString(Path.of("src/main/resources/application.yaml"));

        assertThat(application)
            .contains("default-consumes-media-type: application/json")
            .contains("default-produces-media-type: application/json")
            .contains("api-docs:\n    enabled: true\n    path: /api-docs")
            .contains("swagger-ui:\n    enabled: true\n    path: /swagger-ui.html");
    }

    @Test
    @DisplayName("Swagger UI와 OpenAPI JSON은 인증 없이 문서를 확인할 수 있다")
    void securityAllowsSwaggerUiAndApiDocs() throws IOException {
        String securityConfig = Files.readString(Path.of(
            "src/main/java/com/chapchap/delivery/global/security/SecurityConfig.java"
        ));

        assertThat(securityConfig)
            .contains("\"/api-docs/**\"")
            .contains("\"/swagger-ui/**\"")
            .contains("\"/swagger-ui.html\"")
            .contains(".permitAll()");
    }

    @Test
    @DisplayName("Swagger UI Starter 의존성을 유지한다")
    void buildIncludesSwaggerUiStarter() throws IOException {
        String build = Files.readString(Path.of("build.gradle"));

        assertThat(build)
            .contains("org.springdoc:springdoc-openapi-starter-webmvc-ui");
    }
}
