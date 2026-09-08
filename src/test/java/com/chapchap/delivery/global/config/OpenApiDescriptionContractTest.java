package com.chapchap.delivery.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

class OpenApiDescriptionContractTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
        com.chapchap.delivery.domain.assignment.controller.AdminAssignmentController.class,
        com.chapchap.delivery.domain.assignment.controller.AdminAssignmentIssueController.class,
        com.chapchap.delivery.domain.assignment.controller.AdminDeliveryGroupController.class,
        com.chapchap.delivery.domain.audit.controller.AdminAuditHistoryController.class,
        com.chapchap.delivery.domain.delivery.controller.AdminDeliveryController.class,
        com.chapchap.delivery.domain.delivery.controller.AdminDeliveryOperationController.class,
        com.chapchap.delivery.domain.delivery.controller.AdminIntegrationEventController.class,
        com.chapchap.delivery.domain.delivery.controller.CustomerDeliveryController.class,
        com.chapchap.delivery.domain.delivery.controller.InternalCurrentDeliveryController.class,
        com.chapchap.delivery.domain.rider.controller.RiderController.class,
        com.chapchap.delivery.domain.rider.controller.RiderMeController.class
    );

    @Test
    @DisplayName("모든 REST Controller는 영어 태그명과 한글 설명을 제공한다")
    void everyControllerProvidesEnglishTagNameAndKoreanDescription() {
        CONTROLLERS.forEach(controller -> {
            assertThat(controller.isAnnotationPresent(RestController.class)).isTrue();

            Tag tag = controller.getAnnotation(Tag.class);
            assertThat(tag).isNotNull();
            assertThat(tag.name())
                .containsPattern("[A-Za-z]")
                .doesNotContainPattern("[가-힣]");
            assertThat(tag.description()).containsPattern("[가-힣]");
        });
    }

    @Test
    @DisplayName("모든 공개 API는 영어 기능명과 한글 설명을 제공한다")
    void everyPublicApiProvidesEnglishSummaryAndKoreanDescription() {
        CONTROLLERS.stream()
            .flatMap(controller -> List.of(controller.getDeclaredMethods()).stream())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .forEach(this::assertKoreanOperationDescription);
    }

    @Test
    @DisplayName("모든 공개 API는 상황별 오류 코드 계약을 제공한다")
    void everyPublicApiProvidesErrorCodeContract() {
        CONTROLLERS.stream()
            .flatMap(controller -> List.of(controller.getDeclaredMethods()).stream())
            .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
            .forEach(method -> assertThat(method.getAnnotation(ApiErrorCodes.class))
                .as("%s#%s", method.getDeclaringClass().getSimpleName(), method.getName())
                .isNotNull());
    }

    private void assertKoreanOperationDescription(Method method) {
        Operation operation = method.getAnnotation(Operation.class);

        assertThat(operation)
            .as("%s#%s", method.getDeclaringClass().getSimpleName(), method.getName())
            .isNotNull();
        assertThat(operation.summary())
            .containsPattern("[A-Za-z]")
            .doesNotContainPattern("[가-힣]");
        assertThat(operation.description()).containsPattern("[가-힣]");
    }
}
