package com.campusagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求体。
 *
 * <p>username 和 password 均为必填项，不限制长度，避免泄露密码规则。</p>
 */
@Data
public class LoginRequest {

    /**
     * 用户名，必填。
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码，必填。
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
