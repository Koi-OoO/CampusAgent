package com.campusagent.controller;

import com.campusagent.annotation.RequireRole;
import com.campusagent.common.result.Result;
import com.campusagent.enums.UserRoleEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 临时测试控制器，用于验证权限校验功能，测试通过后删除。
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * 仅管理员（和超管）可访问。
     */
    @RequireRole(UserRoleEnum.ADMIN)
    @GetMapping("/admin")
    public Result<String> adminOnly() {
        return Result.success("管理员/超管可访问");
    }

    /**
     * 仅超管可访问。
     */
    @RequireRole(UserRoleEnum.SUPER_ADMIN)
    @GetMapping("/super")
    public Result<String> superOnly() {
        return Result.success("仅超管可访问");
    }

    /**
     * 所有登录用户可访问。
     */
    @GetMapping("/user")
    public Result<String> userOnly() {
        return Result.success("普通用户可访问");
    }
}