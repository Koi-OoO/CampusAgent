package com.campusagent.constant;

/**
 * 请求属性名常量类，统一管理登录拦截器写入 request 属性的键名。
 *
 * <p>拦截器校验 Token 通过后，通过 {@code request.setAttribute} 写入当前登录用户信息，
 * 控制器通过 {@code @RequestAttribute} 注解按本类常量取出，避免在代码中散落魔法字符串。</p>
 */
public final class RequestAttributeConstants {

    /**
     * 当前登录用户主键，request 属性值为 Long 类型。
     */
    public static final String USER_ID = "userId";

    /**
     * 当前登录用户名，request 属性值为 String 类型。
     */
    public static final String USERNAME = "username";

    /**
     * 当前登录用户角色，request 属性值为 UserRoleEnum 枚举名的字符串，例如 "ADMIN"。
     */
    public static final String ROLE = "role";

    /**
     * 私有构造方法，禁止实例化常量类。
     */
    private RequestAttributeConstants() {
    }
}
