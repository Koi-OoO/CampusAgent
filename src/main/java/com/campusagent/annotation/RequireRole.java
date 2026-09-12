package com.campusagent.annotation;

import com.campusagent.enums.UserRoleEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明访问目标类或方法所允许的用户角色。
 *
 * <p>默认允许管理员和超级管理员访问；方法上的注解优先于类上的注解。
 * 显式指定角色时，仅允许数组中列出的角色，不自动扩展角色层级。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 指定允许访问的角色，当前用户匹配其中任意一个角色即可通过校验。
     *
     * @return 允许访问的角色数组，默认包含管理员和超级管理员；空数组表示拒绝所有角色
     */
    UserRoleEnum[] value() default {UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN};
}
