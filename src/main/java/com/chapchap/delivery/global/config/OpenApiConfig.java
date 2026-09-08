package com.chapchap.delivery.global.config;

import com.chapchap.delivery.DeliveryApplication;
import com.chapchap.delivery.global.exception.ErrorCode;
import com.chapchap.delivery.global.openapi.ApiErrorCodes;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deliveryOpenApi() {
        String version = Optional.ofNullable(
            DeliveryApplication.class.getPackage().getImplementationVersion()
        ).orElse("개발 버전");

        return new OpenAPI()
            .info(new Info()
                .title("Chapchap Delivery Service API")
                .description("도시락 정기 구독 배송의 고객 조회, 기사 배송 수행, 관리자 배정·운영 기능을 제공하는 API입니다.")
                .version(version)
            );
    }

    @Bean
    public OperationCustomizer errorCodeOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (!handlerMethod.getBeanType().getPackageName().startsWith("com.chapchap.delivery")) {
                return operation;
            }

            ApiErrorCodes declared = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
            if (declared == null) {
                return operation;
            }

            boolean internalApi = handlerMethod.getBeanType().getSimpleName()
                .equals("InternalCurrentDeliveryController");
            List<ErrorCode> codes = new ArrayList<>(internalApi
                ? List.of(
                    ErrorCode.INVALID_INTERNAL_SUBJECT,
                    ErrorCode.INTERNAL_SERVICE_AUTHENTICATION_FAILED,
                    ErrorCode.CURRENT_DELIVERY_ACCESS_FORBIDDEN,
                    ErrorCode.INTERNAL_SERVER_ERROR
                )
                : List.of(
                    ErrorCode.INVALID_REQUEST,
                    ErrorCode.AUTHENTICATION_REQUIRED,
                    ErrorCode.DELIVERY_FORBIDDEN,
                    ErrorCode.INTERNAL_SERVER_ERROR
                ));
            codes.addAll(Arrays.asList(declared.value()));

            addErrorResponses(operation, new ArrayList<>(new LinkedHashSet<>(codes)));
            return operation;
        };
    }

    private void addErrorResponses(
        io.swagger.v3.oas.models.Operation operation,
        List<ErrorCode> errorCodes
    ) {
        ApiResponses responses = Optional.ofNullable(operation.getResponses())
            .orElseGet(ApiResponses::new);

        errorCodes.stream()
            .collect(Collectors.groupingBy(
                errorCode -> String.valueOf(errorCode.getHttpStatus().value()),
                LinkedHashMap::new,
                Collectors.toList()
            ))
            .forEach((status, codes) -> responses.addApiResponse(
                status,
                createErrorResponse(codes)
            ));

        operation.setResponses(responses);
    }

    private ApiResponse createErrorResponse(List<ErrorCode> errorCodes) {
        String description = errorCodes.stream()
            .map(errorCode -> "- **%s**: %s".formatted(
                errorCode.getCode(), errorCode.getMessage()
            ))
            .collect(Collectors.joining("\n"));

        io.swagger.v3.oas.models.media.MediaType mediaType =
            new io.swagger.v3.oas.models.media.MediaType();
        errorCodes.forEach(errorCode -> mediaType.addExamples(
            errorCode.getCode(),
            new Example()
                .summary(errorCode.getMessage())
                .value(errorExample(errorCode))
        ));

        return new ApiResponse()
            .description(description)
            .content(new io.swagger.v3.oas.models.media.Content()
                .addMediaType("application/json", mediaType));
    }

    private Map<String, Object> errorExample(ErrorCode errorCode) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("code", errorCode.getCode());
        example.put("message", errorCode.getMessage());
        example.put("data", null);
        return example;
    }
}
