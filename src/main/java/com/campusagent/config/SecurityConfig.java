package com.campusagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全相关配置类，仅提供密码加密器 Bean。
 *
 * <p>项目当前不引入完整的 Spring Security 自动配置，只在需要密码
 * 哈希比较的场景下注册 {@link PasswordEncoder}，供认证服务构造器注入使用。</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * 注册 BCrypt 密码加密器 Bean。
     *
     * @return 基于 BCrypt 算法的密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
