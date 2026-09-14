package com.campusagent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusagent.annotation.RequireRole;
import com.campusagent.common.result.Result;
import com.campusagent.constant.RequestAttributeConstants;
import com.campusagent.dto.request.AdminRoleUpdateRequest;
import com.campusagent.entity.User;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 超管管理控制器，提供超管任命管理员与用户列表分页查询接口。
 *
 * <p>全部接口仅允许超管访问，角色校验由权限切面按类级注解执行；
 * 操作人主键只从登录拦截器验证后写入的请求属性中获取。
 * 用户列表直接返回 User 分页结果，password 字段已在实体上标注
 * {@code @JsonIgnore}，序列化时不会出现在响应体中。</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequireRole(UserRoleEnum.SUPER_ADMIN)
public class AdminController {

    /**
     * 用户服务，负责角色调整与用户分页查询的业务处理。
     */
    private final UserService userService;

    /**
     * 创建超管管理控制器，通过构造器注入用户服务。
     *
     * @param userService 用户服务
     */
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 超管调整指定用户的角色，目标角色仅允许为管理员或超管。
     *
     * @param userId 路径参数中的目标用户主键
     * @param request 角色调整请求体，基础字段由框架统一校验
     * @param operatorId 登录拦截器写入的当前超管主键
     * @return 调整成功响应
     */
    @PostMapping("/users/{userId}/role")
    public Result<Void> updateUserRole(@PathVariable Long userId,
                                       @Valid @RequestBody AdminRoleUpdateRequest request,
                                       @RequestAttribute(RequestAttributeConstants.USER_ID) Long operatorId) {
        userService.updateUserRole(userId, request.getRole(), operatorId);
        return Result.success();
    }

    /**
     * 超管分页查询全部用户，按创建时间倒序返回。
     *
     * <p>响应体中的 User 记录不含 password 字段，密码序列化已在实体层屏蔽。</p>
     *
     * @param page 页码，从 1 开始，默认查询第一页
     * @param size 每页条数，默认为 10
     * @return 用户分页结果，包含记录及总数
     */
    @GetMapping("/users")
    public Result<IPage<User>> listUsers(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                         @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return Result.success(userService.listUsers(page, size));
    }
}
