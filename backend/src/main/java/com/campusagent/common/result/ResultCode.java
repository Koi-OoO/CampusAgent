package com.campusagent.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务状态码，放在响应体的 code 字段中。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    SYSTEM_ERROR(1000, "系统错误"),
    PARAM_ERROR(1001, "参数错误"),
    DATA_NOT_FOUND(1002, "数据不存在"),

    NOT_LOGIN(2000, "未登录"),
    TOKEN_INVALID(2001, "Token 无效或已过期"),
    FORBIDDEN(2002, "无权限访问"),
    LOGIN_FAILED(2003, "用户名或密码错误"),
    USERNAME_EXISTS(2004, "用户名已存在"),

    // 3000–3099 为活动业务错误码区间。
    // 3000 作为通用占位码，3001–3099 预留给后续具体业务错误。
    ACTIVITY_ERROR(3000, "活动相关业务错误"),

    SYSTEM_BUSY(5000, "系统繁忙");

    private final Integer code;
    private final String message;
}
