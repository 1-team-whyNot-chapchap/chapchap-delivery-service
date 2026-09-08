package com.chapchap.delivery.global.config;

import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_NOT_FOUND;
import static com.chapchap.delivery.global.exception.ErrorCode.DELIVERY_STATE_CONFLICT;
import static org.assertj.core.api.Assertions.assertThat;

import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

class OpenApiErrorCodeCustomizerTest {

    @Test
    @DisplayName("HTTP 상태별 실제 오류 코드와 응답 예시를 Swagger에 추가한다")
    void addsErrorCodesAndExamplesGroupedByHttpStatus() throws NoSuchMethodException {
        Operation operation = new Operation().responses(
            new ApiResponses().addApiResponse("200", new ApiResponse().description("성공"))
        );
        Method method = TestApi.class.getDeclaredMethod("execute");
        HandlerMethod handlerMethod = new HandlerMethod(new TestApi(), method);
        OperationCustomizer customizer = new OpenApiConfig().errorCodeOperationCustomizer();

        customizer.customize(operation, handlerMethod);

        assertThat(operation.getResponses()).containsKeys(
            "200", "400", "401", "403", "404", "409", "500"
        );
        assertThat(operation.getResponses().get("404").getDescription())
            .contains("DELIVERY_001")
            .contains("배송 대상을 찾을 수 없습니다.");
        assertThat(operation.getResponses().get("404")
            .getContent().get("application/json").getExamples())
            .containsKey("DELIVERY_001");
        assertThat(operation.getResponses().get("409").getDescription())
            .contains("DELIVERY_006")
            .contains("현재 배송 상태에서는 요청을 처리할 수 없습니다.");
    }

    static class TestApi {
        @ApiErrorCodes({DELIVERY_NOT_FOUND, DELIVERY_STATE_CONFLICT})
        public void execute() {
        }
    }
}
