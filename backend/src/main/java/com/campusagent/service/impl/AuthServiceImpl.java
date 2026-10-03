package com.campusagent.service.impl;

import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.dto.request.LoginRequest;
import com.campusagent.dto.request.RegisterRequest;
import com.campusagent.dto.response.UserInfoResponse;
import com.campusagent.entity.User;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.service.AuthService;
import com.campusagent.service.UserService;
import com.campusagent.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 认证服务实现类，负责注册、登录签发 Token 和当前用户信息查询。
 *
 * <p>密码只以 BCrypt 哈希形式入库，原始密码不落日志。
 * 登录校验按「用户是否存在 → 账号状态 → 密码匹配」的顺序执行，
 * 未命中时统一返回登录失败，避免向调用方泄露具体的失败原因。</p>
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * 用户基础服务，提供按用户名和主键的查询及保存能力。
     */
    private final UserService userService;

    /**
     * JWT 工具类，用于登录成功后签发 Token。
     */
    private final JwtUtil jwtUtil;

    /**
     * 密码加密器，负责注册时加密和登录时比对。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建认证服务实现，依赖全部通过构造器注入。
     *
     * @param userService 用户基础服务
     * @param jwtUtil JWT 工具类
     * @param passwordEncoder 密码加密器
     */
    public AuthServiceImpl(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 注册新用户，用户名唯一性校验通过后保存。
     *
     * @param request 注册请求体，密码在保存前加密
     * @return 注册成功后的用户信息，不包含密码
     */
    @Override
    public UserInfoResponse register(RegisterRequest request) {
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        // 用户名唯一性校验放在事务提交前，避免数据库唯一约束触发后的处理成本。
        User existing = userService.getByUsername(request.getUsername());
        // 用户名已存在时直接终止注册流程。
        if (existing != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        if (userService.getByStudentId(request.getStudentId()) != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "学号已注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        // 只保存密码哈希，保证数据库泄露时无法还原明文密码。
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setStudentId(request.getStudentId());
        user.setCollege(request.getCollege());
        user.setPhone(request.getPhone());
        // 注册用户默认授予普通用户角色。
        user.setRole(UserRoleEnum.USER);
        // 新注册账号默认启用。
        user.setStatus(1);

        // 应用层预检查之外，数据库唯一约束负责兜住并发注册。
        try {
            userService.save(user);
        } catch (DataIntegrityViolationException exception) {
            log.warn("注册触发唯一约束，用户名：{}，学号：{}", user.getUsername(), user.getStudentId());
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户名或学号已注册");
        }
        log.info("用户注册成功，用户名：{}，用户主键：{}", user.getUsername(), user.getId());
        // 响应体中不包含密码字段。
        return buildUserInfoResponse(user);
    }

    /**
     * 用户登录，校验通过后签发携带用户主键、用户名和角色的 Token。
     *
     * @param request 登录请求体，包含用户名和密码
     * @return 签发的 JWT Token 字符串
     */
    @Override
    public String login(LoginRequest request) {
        // 按用户名查询未逻辑删除的用户，用户名错误和密码错误返回同一错误码。
        User user = userService.getByUsername(request.getUsername());
        // 用户不存在时使用登录失败码，不提示是用户名还是密码的问题。
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 禁用账号不允许登录。
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 密码不匹配时同样返回登录失败码。
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 登录成功后将用户主键、用户名和角色写入 Token，供拦截器还原请求属性。
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("用户登录成功，用户名：{}，用户主键：{}", user.getUsername(), user.getId());
        return token;
    }

    /**
     * 根据用户主键查询当前登录用户信息。
     *
     * @param userId 用户主键，由登录拦截器写入请求属性
     * @return 当前用户信息，不包含密码
     */
    @Override
    public UserInfoResponse getCurrentUser(Long userId) {
        // 用户主键来自已校验的 Token，正常情况下必然能查到。
        User user = userService.getById(userId);
        // 用户被删除或数据异常时返回统一的数据不存在错误。
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return buildUserInfoResponse(user);
    }

    /**
     * 构建对外返回的用户信息响应体。
     *
     * @param user 用户实体
     * @return 不含密码的用户信息响应体
     */
    private UserInfoResponse buildUserInfoResponse(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setStudentId(user.getStudentId());
        response.setCollege(user.getCollege());
        response.setPhone(user.getPhone());
        response.setAvatar(user.getAvatar());
        response.setRole(user.getRole());
        return response;
    }
}
