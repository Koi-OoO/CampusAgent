package com.campusagent.common;

import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.exception.GlobalExceptionHandler;
import com.campusagent.common.result.Result;
import com.campusagent.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CommonComponentsTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(new TestController())
                .setControllerAdvice(exceptionHandler)
                .setValidator(validator)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
    }

    @Test
    void returnsSuccessfulPayloadInTheStandardJsonEnvelope() throws Exception {
        mockMvc.perform(get("/test/common/success"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"code":200,"message":"操作成功","data":{"id":42,"name":"校园讲座"}}
                        """, true));
    }

    @Test
    void supportsSuccessfulResponsesWithoutDataAndWithCustomMessages() throws Exception {
        mockMvc.perform(get("/test/common/success/empty"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":200,"message":"操作成功","data":null}
                        """, true));

        mockMvc.perform(get("/test/common/success/custom"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":200,"message":"报名成功","data":{"status":"accepted"}}
                        """, true));
    }

    @Test
    void preservesTheDefaultBusinessErrorCodeAndMessage() throws Exception {
        mockMvc.perform(get("/test/common/business/default"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":1002,"message":"数据不存在","data":null}
                        """, true));
    }

    @Test
    void preservesTheCustomBusinessErrorMessage() throws Exception {
        mockMvc.perform(get("/test/common/business/custom"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":3000,"message":"活动报名已截止","data":null}
                        """, true));
    }

    @Test
    void aggregatesActualRequestBodyValidationMessagesWithoutDuplicates() throws Exception {
        MvcResult response = mockMvc.perform(post("/test/common/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","description":"","quantity":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andReturn();

        String message = new ObjectMapper()
                .readTree(response.getResponse().getContentAsString())
                .path("message").asText();
        assertThat(message.split("；"))
                .containsExactlyInAnyOrder("必填项不能为空", "数量必须大于零");
    }

    @Test
    void masksUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/test/common/unexpected"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":1000,"message":"系统错误","data":null}
                        """, true));
    }

    @Test
    void includesObjectErrorsAndIgnoresBlankValidationMessages() throws Exception {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new FieldError("request", "name", "姓名不能为空"));
        errors.addError(new ObjectError("request", "开始时间必须早于结束时间"));
        errors.addError(new ObjectError("request", "姓名不能为空"));
        errors.addError(new ObjectError("request", " \t "));
        errors.addError(new ObjectError("request", null));

        Result<Void> response = exceptionHandler.handleMethodArgumentNotValidException(invalidArguments(errors));

        assertThat(response.getCode()).isEqualTo(1001);
        assertThat(response.getMessage()).isEqualTo("姓名不能为空；开始时间必须早于结束时间");
        assertThat(response.getData()).isNull();
    }

    @Test
    void usesDefaultParameterMessageWhenNoUsableValidationMessageExists() throws Exception {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new FieldError("request", "name", null));
        errors.addError(new ObjectError("request", ""));
        errors.addError(new ObjectError("request", " \t "));

        Result<Void> response = exceptionHandler.handleMethodArgumentNotValidException(invalidArguments(errors));

        assertThat(response.getCode()).isEqualTo(1001);
        assertThat(response.getMessage()).isEqualTo("参数错误");
        assertThat(response.getData()).isNull();
    }

    private static MethodArgumentNotValidException invalidArguments(BeanPropertyBindingResult errors)
            throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(
                TestController.class.getDeclaredMethod("validate", ValidationRequest.class), 0);
        return new MethodArgumentNotValidException(parameter, errors);
    }

    public record ValidationRequest(
            @NotBlank(message = "必填项不能为空") String name,
            @NotBlank(message = "必填项不能为空") String description,
            @Min(value = 1, message = "数量必须大于零") int quantity) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/common/success")
        public Result<Map<String, Object>> success() {
            return Result.success(Map.of("id", 42, "name", "校园讲座"));
        }

        @GetMapping("/test/common/success/empty")
        public Result<Void> emptySuccess() {
            return Result.success();
        }

        @GetMapping("/test/common/success/custom")
        public Result<Map<String, String>> customSuccess() {
            return Result.success("报名成功", Map.of("status", "accepted"));
        }

        @GetMapping("/test/common/business/default")
        public Result<Void> defaultBusinessError() {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        @GetMapping("/test/common/business/custom")
        public Result<Void> customBusinessError() {
            throw new BusinessException(ResultCode.ACTIVITY_ERROR, "活动报名已截止");
        }

        @PostMapping("/test/common/validate")
        public Result<ValidationRequest> validate(@Valid @RequestBody ValidationRequest request) {
            return Result.success(request);
        }

        @GetMapping("/test/common/unexpected")
        public Result<Void> unexpectedError() {
            throw new IllegalStateException("Internal storage endpoint: db.internal");
        }
    }
}
