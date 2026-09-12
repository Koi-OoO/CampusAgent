package com.campusagent.service;

import com.campusagent.dto.request.LoginRequest;
import com.campusagent.dto.request.RegisterRequest;
import com.campusagent.dto.response.UserInfoResponse;

/**
 * 认证服务接口，提供注册、登录和当前用户信息查询能力。
 */
public interface AuthService {

    /**
     * 注册新用户。
     *
     * @param request 注册请求体，用户名不能与已有用户重复
     * @return 注册成功后的用户信息
     */
    UserInfoResponse register(RegisterRequest request);

    /**
     * 用户登录，校验通过后签发 JWT。
     *
     * @param request 登录请求体，包含用户名和密码
     * @return 签发的 JWT Token 字符串
     */
    String login(LoginRequest request);

    /**
     * 根据用户主键查询当前登录用户信息。
     *
     * @param userId 用户主键，由登录拦截器写入请求属性
     * @return 当前用户信息
     */
    UserInfoResponse getCurrentUser(Long userId);
}
