package com.campusagent.interceptor;

import cn.hutool.json.JSONUtil;
import com.campusagent.common.result.Result;
import com.campusagent.common.result.ResultCode;
import com.campusagent.constant.RequestAttributeConstants;
import com.campusagent.enums.UserRoleEnum;
import com.campusagent.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 登录拦截器，校验请求头中的 Bearer Token，并将登录用户信息写入 request 属性。
 *
 * <p>白名单和拦截路径由 WebConfig 通过构造器统一传入。
 * Token 校验通过后，通过 {@code request.setAttribute} 存入 userId、username 和 role，
 * 控制器使用 {@code @RequestAttribute} 取出后再显式传给 Service，不使用 ThreadLocal 保存用户信息。</p>
 */
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    // Authorization 请求头中的 Bearer 认证方案前缀，包含与 Token 分隔的空格。
    private static final String BEARER_PREFIX = "Bearer ";

    // 无需登录校验的精确路径列表，由 WebConfig 提供。
    private final List<String> whiteListPaths;

    // 需要进入登录拦截流程的路径模式列表，由 WebConfig 提供。
    private final List<String> interceptPaths;

    // JWT 工具类，负责 Token 的解析和校验。
    private final JwtUtil jwtUtil;

    // 匹配 /api/** 等 Ant 风格路径模式的工具。
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 创建登录拦截器，并保存 WebConfig 传入的路径配置和 JwtUtil。
     *
     * @param jwtUtil JWT 工具类，不能为 null
     * @param whiteListPaths 无需登录校验的精确路径列表，列表及其元素均不能为 null
     * @param interceptPaths 需要拦截的 Ant 风格路径模式列表，列表及其元素均不能为 null
     */
    public LoginInterceptor(JwtUtil jwtUtil, List<String> whiteListPaths, List<String> interceptPaths) {
        // 保存 JwtUtil，供 Token 校验和解析使用。
        this.jwtUtil = jwtUtil;
        // 保存白名单的只读副本，避免外部修改集合影响放行规则。
        this.whiteListPaths = List.copyOf(whiteListPaths);
        // 保存拦截路径的只读副本，确保运行期间使用稳定的匹配规则。
        this.interceptPaths = List.copyOf(interceptPaths);
    }

    /**
     * 在控制器执行前校验登录状态，并把用户信息写入 request 属性。
     *
     * <p>白名单路径直接放行；其余拦截范围内的请求必须携带有效的 Bearer Token，
     * 否则返回统一格式的未登录或 Token 无效响应并中断请求。</p>
     *
     * @param request 当前 HTTP 请求，用于读取请求路径、请求头并写入登录用户属性
     * @param response 当前 HTTP 响应，用于写出未登录或 Token 无效的提示
     * @param handler 即将执行的请求处理器，通常为控制器方法
     * @return Token 有效返回 true，请求继续执行；否则返回 false，请求被拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 去除应用上下文路径，使应用部署在非根路径时仍可匹配 /api/** 配置。
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());

        // 白名单接口直接放行，无需读取或解析 Token。
        if (whiteListPaths.contains(requestPath)) {
            return true;
        }

        // 复用传入的拦截规则，确保独立调用拦截器时也只处理指定范围内的路径。
        boolean intercepted = interceptPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
        // 不属于拦截范围的请求直接放行。
        if (!intercepted) {
            return true;
        }

        // 提取标准 Authorization 请求头中的 Bearer Token。
        String token = extractToken(request);
        // 未携带 Token 时直接返回统一格式的未登录响应，并中断请求。
        if (!StringUtils.hasText(token)) {
            log.warn("请求未携带 Bearer Token，已拦截，请求路径：{}", requestPath);
            return writeUnauthorizedResponse(response, ResultCode.NOT_LOGIN);
        }

        // Token 签名不符或已过期时，返回统一格式的 Token 无效响应。
        if (!jwtUtil.validateToken(token)) {
            log.warn("请求携带的 Token 无效或已过期，已拦截，请求路径：{}", requestPath);
            return writeUnauthorizedResponse(response, ResultCode.TOKEN_INVALID);
        }

        // 取出 Token 中的用户主键，并保存为请求属性。
        request.setAttribute(RequestAttributeConstants.USER_ID, jwtUtil.getUserId(token));
        // 取出 Token 中的用户名，并保存为请求属性。
        request.setAttribute(RequestAttributeConstants.USERNAME, jwtUtil.getUsername(token));
        // 取出 Token 中的角色，并保存为枚举名的字符串形式。
        UserRoleEnum role = jwtUtil.getRole(token);
        request.setAttribute(RequestAttributeConstants.ROLE, role == null ? null : role.name());

        // Token 有效，放行至控制器继续处理。
        return true;
    }

    /**
     * 从 Authorization 请求头中提取 Bearer Token。
     *
     * @param request 当前 HTTP 请求
     * @return 提取出的 Token；未携带请求头、认证方案不匹配或 Token 为空白时返回 null
     */
    private String extractToken(HttpServletRequest request) {
        // 读取标准 Authorization 请求头，约定格式为 Bearer {token}。
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        // 未携带请求头时直接返回 null。
        if (authorization == null) {
            return null;
        }
        // 认证方案名称不区分大小写，同时要求 Bearer 后存在分隔空格。
        if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        // 去除 Bearer 前缀及 Token 两端空白，避免将空字符串视为有效内容。
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        // 纯空白的 Token 视为未携带，交由调用方拦截。
        return StringUtils.hasText(token) ? token : null;
    }

    /**
     * 写出统一格式的未登录响应，并返回拦截结果。
     *
     * @param response 当前 HTTP 响应
     * @param resultCode 提示用的状态码，一般为 NOT_LOGIN 或 TOKEN_INVALID
     * @return 始终返回 false，表示请求被拦截
     * @throws Exception 响应写出过程中的 IO 异常
     */
    private boolean writeUnauthorizedResponse(HttpServletResponse response, ResultCode resultCode) throws Exception {
        // 声明响应体为 JSON 格式，避免浏览器按 HTML 解析。
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=" + StandardCharsets.UTF_8);
        // 使用 Hutool 的 JSONUtil 将统一响应对象序列化后写出，序列化遵循字段的 getter。
        response.getWriter().write(JSONUtil.toJsonStr(Result.error(resultCode.getCode(), resultCode.getMessage())));
        // 返回 false 中断请求，不再进入后续拦截器和控制器。
        return false;
    }
}
