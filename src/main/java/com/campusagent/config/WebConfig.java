package com.campusagent.config;

import com.campusagent.interceptor.LoginInterceptor;
import com.campusagent.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 配置类，统一配置跨域访问和登录拦截器。
 *
 * <p>拦截路径和白名单使用固定配置，登录拦截器通过构造器传入 JwtUtil、
 * 白名单和拦截路径后手动创建。登录拦截器负责校验 Token，
 * 并将登录用户信息写入 request 属性，不使用 ThreadLocal 保存用户信息。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 登录拦截器的生效路径，匹配所有以 /api 开头的接口路径。
    private static final List<String> INTERCEPT_PATHS = List.of("/api/**");

    // 无需登录即可访问的接口白名单，当前固定为登录和注册接口。
    private static final List<String> WHITE_LIST_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/category/list",
            "/api/template/list"
    );

    // JWT 工具类，用于拦截器中 Token 的解析和校验。
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 配置跨域规则，允许任意来源、任意请求方法及携带凭证的请求。
     *
     * @param registry Spring MVC 跨域配置注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 对所有请求路径应用跨域规则。
        registry.addMapping("/**")
                // 使用来源模式匹配任意来源，兼容允许携带凭证的配置。
                .allowedOriginPatterns("*")
                // 允许所有 HTTP 请求方法，包括跨域预检请求声明的方法。
                .allowedMethods("*")
                // 允许所有请求头，包括前端传递 Token 时使用的 Authorization。
                .allowedHeaders("*")
                // 允许浏览器在跨域请求中携带 Cookie 等凭证。
                .allowCredentials(true);
    }

    /**
     * 注册登录拦截器，并设置拦截路径和白名单路径。
     *
     * @param registry Spring MVC 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 手动创建登录拦截器，将 JwtUtil、白名单和拦截路径通过构造器传入。
        LoginInterceptor loginInterceptor = new LoginInterceptor(jwtUtil, WHITE_LIST_PATHS, INTERCEPT_PATHS);

        // 将拦截器加入 Spring MVC 的请求处理链。
        registry.addInterceptor(loginInterceptor)
                // 对所有 /api/** 路径启用登录拦截器。
                .addPathPatterns(INTERCEPT_PATHS)
                // 登录和注册接口直接放行，不进入登录校验流程。
                .excludePathPatterns(WHITE_LIST_PATHS);
    }
}
