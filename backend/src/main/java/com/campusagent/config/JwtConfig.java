package com.campusagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置类，绑定配置文件中 jwt 前缀下的配置项。
 *
 * <p>通过 {@link ConfigurationProperties} 将 application.yml 中的
 * jwt.secret 和 jwt.expiration 绑定到本类字段，供 JwtUtil 统一读取。
 * {@link Component} 使本类注册为 Spring Bean，从而被容器扫描并完成属性绑定。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * 签名密钥的 Base64 编码字符串，解码后长度不得低于 HS256 算法要求的 32 字节。
     */
    private String secret;

    /**
     * Token 有效时长，单位为毫秒。
     */
    private Long expiration;
}
