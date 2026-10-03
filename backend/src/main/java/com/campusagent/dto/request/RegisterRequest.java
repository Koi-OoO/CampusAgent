package com.campusagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求体。
 *
 * <p>注册时必须提交完整校园身份信息，并由后端统一校验。
 * phone 为空时不校验格式，非空时必须匹配中国大陆手机号规则。</p>
 */
@Data
public class RegisterRequest {

    /**
     * 用户名，必填，长度在 3 到 50 之间。
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3 到 50 之间")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,49}$",
            message = "用户名只能包含字母、数字和下划线，且必须以字母开头"
    )
    private String username;

    /**
     * 密码，必填，长度在 6 到 50 之间。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在 6 到 50 之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
    private String password;

    /**
     * 真实姓名，必填，最长 50 个字符。
     */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过 50")
    private String realName;

    /**
     * 学号，必填，只允许字母和数字，最长 20 个字符。
     */
    @NotBlank(message = "学号不能为空")
    @Size(max = 20, message = "学号长度不能超过 20")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "学号只能包含字母和数字")
    private String studentId;

    /**
     * 所属学院，必填，最长 100 个字符。
     */
    @NotBlank(message = "学院不能为空")
    @Size(max = 100, message = "学院长度不能超过 100")
    private String college;

    /**
     * 确认密码，由服务层校验必须与 password 一致。
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 联系电话，允许为空（null 或空字符串），非空时必须是合法的大陆手机号。
     *
     * <p>@Pattern 对 null 值直接放行，但空字符串会参与正则匹配，
     * 因此正则整体使用可选分组，使空字符串同样通过校验。</p>
     */
    @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "手机号格式不正确")
    private String phone;
}
