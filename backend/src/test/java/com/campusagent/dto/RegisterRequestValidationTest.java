package com.campusagent.dto;

import com.campusagent.dto.request.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 注册请求体校验测试，验证手机号字段对空值的处理规则。
 *
 * <p>手机号允许为 null 或空字符串，此时不参与格式校验；
 * 非空时必须匹配中国大陆手机号格式。</p>
 */
class RegisterRequestValidationTest {

    // 独立构建校验器，不依赖 Spring 容器。
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsNullPhone() {
        RegisterRequest request = validRequest();
        request.setPhone(null);
        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void acceptsEmptyPhone() {
        RegisterRequest request = validRequest();
        request.setPhone("");
        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void acceptsValidPhone() {
        RegisterRequest request = validRequest();
        request.setPhone("13800138000");
        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingRealName() {
        RegisterRequest request = validRequest();
        request.setRealName("");
        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("真实姓名不能为空");
    }

    @Test
    void rejectsMissingStudentId() {
        RegisterRequest request = validRequest();
        request.setStudentId("");
        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("学号不能为空");
    }

    @Test
    void rejectsMissingCollege() {
        RegisterRequest request = validRequest();
        request.setCollege("");
        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("学院不能为空");
    }

    @Test
    void rejectsUsernameWithUnsupportedCharacters() {
        RegisterRequest request = validRequest();
        request.setUsername("中文用户名");
        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("用户名只能包含字母、数字和下划线，且必须以字母开头");
    }

    @Test
    void rejectsWeakPassword() {
        RegisterRequest request = validRequest();
        request.setPassword("123456");
        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("密码必须同时包含字母和数字");
    }

    @Test
    void rejectsInvalidStudentId() {
        RegisterRequest request = validRequest();
        request.setStudentId("学号-001");
        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("学号只能包含字母和数字");
    }

    @Test
    void rejectsInvalidPhone() {
        RegisterRequest request = validRequest();
        request.setPhone("12345");
        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getMessage())
                .containsExactly("手机号格式不正确");
    }

    /**
     * 构建通过全部校验的注册请求，作为各测试的基础数据。
     *
     * @return 字段合法的注册请求
     */
    private static RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("abc123");
        request.setConfirmPassword("abc123");
        request.setRealName("测试用户");
        request.setStudentId("20260001");
        request.setCollege("计算机学院");
        return request;
    }
}
