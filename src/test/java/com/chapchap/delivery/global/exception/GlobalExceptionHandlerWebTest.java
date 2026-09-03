package com.chapchap.delivery.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerWebTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerWebTest.TestController.class)
class GlobalExceptionHandlerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("요청 DTO 검증 실패 시 COMMON_001을 반환한다")
    void invalidRequestReturnsCommon001() throws Exception {
        mockMvc.perform(
                post("/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "name": ""
                        }
                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_001"))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
            .andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    @DisplayName("잘못된 JSON 요청 시 COMMON_001을 반환한다")
    void invalidJsonReturnsCommon001() throws Exception {
        mockMvc.perform(
                post("/test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "name":
                        }
                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_001"))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
            .andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    @DisplayName("PathVariable 타입이 올바르지 않으면 COMMON_001을 반환한다")
    void typeMismatchReturnsCommon001() throws Exception {
        mockMvc.perform(
                get("/test/not-number")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_001"))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
            .andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    @DisplayName("필수 요청 파라미터가 누락되면 COMMON_001을 반환한다")
    void missingRequestParameterReturnsCommon001() throws Exception {
        mockMvc.perform(
                get("/test-param")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_001"))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
            .andExpect(jsonPath("$.data").value((Object) null));
    }

    @Test
    @DisplayName("존재하지 않는 URL 요청 시 COMMON_003을 반환한다")
    void noResourceFoundReturnsCommon003() throws Exception {
        mockMvc.perform(
                get("/not-exist")
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COMMON_003"))
            .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").value((Object) null));
    }

    @RestController
    static class TestController {

        @PostMapping("/test")
        void test(
            @Valid @RequestBody TestRequest request
        ) {
        }

        @GetMapping("/test/{id}")
        void testPathVariable(
            @PathVariable Long id
        ) {
        }

        @GetMapping("/test-param")
        void testRequestParam(
            @RequestParam String value
        ) {
        }
    }

    record TestRequest(
        @NotBlank String name
    ) {
    }
}