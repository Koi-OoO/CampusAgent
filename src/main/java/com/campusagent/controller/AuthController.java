package com.campusagent.controller;

import com.campusagent.common.result.Result;
import com.campusagent.constant.RequestAttributeConstants;
import com.campusagent.dto.request.LoginRequest;
import com.campusagent.dto.request.RegisterRequest;
import com.campusagent.dto.response.UserInfoResponse;
import com.campusagent.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器，提供注册、登录、当前用户信息查询和退出登录接口。
 *
 * <p>注册和登录接口位于拦截器白名单中；/me 需要登录，
 * 用户主键由登录拦截器写入请求属性，再通过 @RequestAttribute 取出后显式传给服务层。
 * 退出登录暂未接入 Redis 黑名单，当前只返回成功占位。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 认证服务，依赖通过构造器注入。
     */
    private final AuthService authService;

    /**
     * 创建认证控制器。
     *
     * @param authService 认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册接口。
     *
     * @param request 注册请求体，参数合法性由框架统一校验
     * @return 注册成功后的用户信息
     */
    @PostMapping("/register")
    public Result<UserInfoResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 参数校验失败时由全局异常处理器统一返回参数错误响应。
        return Result.success(authService.register(request));
    }

    /**
     * 用户登录接口。
     *
     * @param request 登录请求体，包含用户名和密码
     * @return 登录成功后签发的 Token
     */
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginRequest request) {
        // Token 由服务层签发，控制器只负责透传。
        return Result.success(authService.login(request));
    }

    /**
     * 当前登录用户信息接口。
     *
     * @param userId 用户主键，由登录拦截器写入请求属性
     * @return 当前登录用户的资料信息
     */
    @GetMapping("/me")
    public Result<UserInfoResponse> me(@RequestAttribute(RequestAttributeConstants.USER_ID) Long userId) {
        // 用户主键来自拦截器解析的 Token，直接交给服务层查询。
        return Result.success(authService.getCurrentUser(userId));
    }

    /**
     * 退出登录接口。
     *
     * <p>当前为占位实现，后续接入 Redis Token 黑名单后在此使 Token 失效。</p>
     *
     * @return 退出成功响应
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // TODO 后续接入 Redis 黑名单，将当前 Token 标记为失效。
        return Result.success();
    }
}
