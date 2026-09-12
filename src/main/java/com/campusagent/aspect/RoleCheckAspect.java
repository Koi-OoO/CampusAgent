package com.campusagent.aspect;

import com.campusagent.annotation.RequireRole;
import com.campusagent.common.exception.BusinessException;
import com.campusagent.common.result.ResultCode;
import com.campusagent.constant.RequestAttributeConstants;
import com.campusagent.enums.UserRoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 角色权限校验切面，在带有权限声明的方法执行前检查当前用户角色。
 *
 * <p>登录拦截器负责认证并写入请求属性，本切面读取角色后直接放行超级管理员，其他角色进行精确匹配。
 * 方法上的权限注解优先于类上的注解，校验失败时通过业务异常交由统一异常处理器生成响应。</p>
 */
@Slf4j
@Aspect
@Component
public class RoleCheckAspect {

    /**
     * 在受保护的方法执行前校验当前用户角色，超级管理员直接放行，其他角色按权限注解精确匹配。
     *
     * @param joinPoint 当前方法的连接点，包含目标对象和方法签名
     * @throws BusinessException 未登录、角色非法或当前角色无权访问时抛出
     */
    @Before("@annotation(com.campusagent.annotation.RequireRole) || "
            + "@within(com.campusagent.annotation.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        UserRoleEnum currentRole = getCurrentRole();
        // 超管拥有所有权限，直接放行
        if (currentRole == UserRoleEnum.SUPER_ADMIN) {
            return;
        }

        RequireRole requireRole = resolveRequireRole(joinPoint);

        // 逐个匹配显式允许的角色；空数组拒绝所有非超管角色。
        for (UserRoleEnum allowedRole : requireRole.value()) {
            if (currentRole == allowedRole) {
                return;
            }
        }

        log.warn("角色权限校验未通过，当前角色：{}，目标方法：{}",
                currentRole, joinPoint.getSignature().toShortString());
        throw new BusinessException(ResultCode.FORBIDDEN);
    }

    /**
     * 从当前 HTTP 请求中读取登录拦截器写入的角色枚举名称，并转换为角色枚举。
     *
     * @return 当前请求中的合法用户角色
     * @throws BusinessException 缺少请求或角色时抛出未登录异常，角色类型或名称非法时抛出无权限异常
     */
    private UserRoleEnum getCurrentRole() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            log.warn("角色权限校验失败：缺少 HTTP 请求上下文");
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        Object roleAttribute = request.getAttribute(RequestAttributeConstants.ROLE);
        if (roleAttribute == null) {
            log.warn("角色权限校验失败：请求中缺少用户角色");
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }
        if (!(roleAttribute instanceof String roleName)) {
            log.warn("角色权限校验失败：请求中的角色属性类型不正确");
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!StringUtils.hasText(roleName)) {
            log.warn("角色权限校验失败：请求中的用户角色为空");
            throw new BusinessException(ResultCode.NOT_LOGIN);
        }

        try {
            // 请求属性保存的是枚举名称，不使用数据库角色编码进行转换。
            return UserRoleEnum.valueOf(roleName);
        } catch (IllegalArgumentException exception) {
            // 非法角色直接拒绝访问，避免枚举转换异常被当作系统错误返回。
            log.warn("角色权限校验失败：请求中的角色名称无法识别");
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    /**
     * 解析目标方法或目标类上的权限注解，优先使用方法级别的声明。
     *
     * @param joinPoint 当前方法的连接点，用于获取真实目标类型和具体方法
     * @return 当前调用生效的权限注解
     * @throws BusinessException 无法解析受保护方法的权限声明时抛出无权限异常
     */
    private RequireRole resolveRequireRole(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = AopUtils.getTargetClass(joinPoint.getTarget());
        // 定位实际实现方法，兼容代理暴露的方法签名与目标类方法不一致的情况。
        Method targetMethod = AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);
        RequireRole methodRole = AnnotatedElementUtils.findMergedAnnotation(targetMethod, RequireRole.class);
        if (methodRole != null) {
            return methodRole;
        }

        RequireRole classRole = AnnotatedElementUtils.findMergedAnnotation(targetClass, RequireRole.class);
        if (classRole != null) {
            return classRole;
        }

        // 已进入权限切点却无法获取声明时拒绝访问，避免异常配置导致放行。
        log.error("无法解析角色权限注解，目标方法：{}", signature.toShortString());
        throw new BusinessException(ResultCode.FORBIDDEN);
    }
}
