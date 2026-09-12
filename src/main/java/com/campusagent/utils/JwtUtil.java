package com.campusagent.utils;

import com.campusagent.config.JwtConfig;
import com.campusagent.enums.UserRoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 工具类，负责 Token 的生成、解析和校验。
 *
 * <p>基于 jjwt 0.12.6 API 实现，密钥由 JwtConfig 中的 Base64 编码字符串解码得到。
 * 签发时 userId、username 写入自定义声明，role 以 UserRoleEnum 枚举名的字符串形式写入；
 * 所有签名密钥均来自配置，不在代码中硬编码任何密钥常量。</p>
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * Token 自定义声明中用户主键的键名。
     */
    private static final String CLAIM_USER_ID = "userId";

    /**
     * Token 自定义声明中用户名的键名。
     */
    private static final String CLAIM_USERNAME = "username";

    /**
     * Token 自定义声明中角色枚举名的键名。
     */
    private static final String CLAIM_ROLE = "role";

    /**
     * JWT 配置，由 Spring 构造器注入。
     */
    private final JwtConfig jwtConfig;

    /**
     * 解析后的签名密钥，由配置中的 Base64 编码密钥解码而来。
     */
    private final SecretKey secretKey;

    /**
     * 创建 JWT 工具类，并预解析签名密钥。
     *
     * @param jwtConfig JWT 配置，提供签名密钥和有效时长
     */
    public JwtUtil(JwtConfig jwtConfig) {
        // 保存配置引用，供签发和解析 Token 时读取有效时长。
        this.jwtConfig = jwtConfig;
        // 构造时一次性解码并缓存签名密钥，避免每次操作重复解析。
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtConfig.getSecret()));
    }

    /**
     * 生成登录 Token，不携带角色信息。
     *
     * <p>保留无角色的签发入口，方便测试等不需要角色声明的场景。</p>
     *
     * @param userId 用户主键
     * @param username 用户名
     * @return 已签名的 JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, null);
    }

    /**
     * 生成登录 Token，并携带用户角色信息。
     *
     * @param userId 用户主键
     * @param username 用户名
     * @param role 用户角色；允许为 null，此时 Token 不携带角色声明
     * @return 已签名的 JWT 字符串
     */
    public String generateToken(Long userId, String username, UserRoleEnum role) {
        // 以当前时间为签发时间和生效时间。
        Date now = new Date();
        // 过期时间 = 当前时间 + 配置的有效时长。
        Date expireDate = new Date(now.getTime() + jwtConfig.getExpiration());
        return Jwts.builder()
                // 写入用户主键声明。
                .claim(CLAIM_USER_ID, userId)
                // 写入用户名声明。
                .claim(CLAIM_USERNAME, username)
                // role 为 null 时跳过声明，避免 Token 中出现空值角色。
                .claim(CLAIM_ROLE, role == null ? null : role.name())
                // 设置签发时间。
                .issuedAt(now)
                // 设置过期时间。
                .expiration(expireDate)
                // 使用 HS256 密钥签名并生成紧凑格式的 JWT。
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 Token 的签名和有效期，返回载荷声明。
     *
     * <p>签名不匹配或已过期时抛出对应的 JWT 异常，不在此处吞掉异常。</p>
     *
     * @param token 待解析的 JWT 字符串，不能为空
     * @return 解析成功后的载荷声明
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                // 指定签名校验所用的密钥。
                .verifyWith(secretKey)
                // 解析出载荷内容，签名不符或过期时由框架抛出异常。
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中取出用户主键。
     *
     * @param token 待解析的 JWT 字符串
     * @return 用户主键，声明缺失时返回 null
     */
    public Long getUserId(String token) {
        // 声明值可能超出 Integer 范围，统一按 Number 读取后转为 Long。
        Number userId = parseToken(token).get(CLAIM_USER_ID, Number.class);
        // 声明缺失时返回 null，由调用方判断。
        return userId == null ? null : userId.longValue();
    }

    /**
     * 从 Token 中取出用户名。
     *
     * @param token 待解析的 JWT 字符串
     * @return 用户名，声明缺失时返回 null
     */
    public String getUsername(String token) {
        return parseToken(token).get(CLAIM_USERNAME, String.class);
    }

    /**
     * 从 Token 中取出角色，并还原为 UserRoleEnum 枚举。
     *
     * @param token 待解析的 JWT 字符串
     * @return 用户角色；声明缺失或枚举名不合法时返回 null
     */
    public UserRoleEnum getRole(String token) {
        // 角色在 Token 中以枚举名 String 形式存储，按名称还原枚举。
        String roleName = parseToken(token).get(CLAIM_ROLE, String.class);
        // 无角色声明时直接返回 null。
        if (roleName == null) {
            return null;
        }
        // 传入的是签发的合法枚举名，valueOf 不会抛出异常，无需额外处理。
        return UserRoleEnum.valueOf(roleName);
    }

    /**
     * 校验 Token 是否有效。
     *
     * @param token 待校验的 JWT 字符串
     * @return Token 有效返回 true；为空白、签名不符或已过期时返回 false
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            // 解析成功即视为 Token 有效。
            parseToken(token);
            return true;
        } catch (Exception exception) {
            // 记录签名或过期信息用于问题排查，避免输出 Token 原文。
            log.debug("Token 校验失败：{}", exception.getMessage());
            return false;
        }
    }

    /**
     * 获取 Token 的剩余有效时间。
     *
     * @param token 待解析的 JWT 字符串
     * @return 剩余有效毫秒数，已过期或解析失败时返回 0
     */
    public long getRemainingTime(String token) {
        // 计算过期时间与当前时间的差值。
        long remaining = parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
        // 已过期时统一返回 0，避免出现负的剩余时长。
        return Math.max(remaining, 0);
    }
}
