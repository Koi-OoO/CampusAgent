package com.campusagent.service;

import com.campusagent.common.exception.BusinessException;
import com.campusagent.dto.request.RegisterRequest;
import com.campusagent.service.impl.AuthServiceImpl;
import com.campusagent.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务注册业务测试，验证确认密码和学号唯一性由服务层兜底。
 */
class AuthServiceImplTest {

    @Test
    void rejectsMismatchedConfirmPassword() {
        UserService userService = mock(UserService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthServiceImpl authService = new AuthServiceImpl(userService, jwtUtil, passwordEncoder);

        RegisterRequest request = validRequest();
        request.setConfirmPassword("different123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("两次输入的密码不一致");
        verify(userService, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDuplicateStudentId() {
        UserService userService = mock(UserService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthServiceImpl authService = new AuthServiceImpl(userService, jwtUtil, passwordEncoder);

        RegisterRequest request = validRequest();
        when(userService.getByUsername(request.getUsername())).thenReturn(null);
        when(userService.getByStudentId(request.getStudentId())).thenReturn(new com.campusagent.entity.User());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("学号已注册");
        verify(userService, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("abc123");
        request.setConfirmPassword("abc123");
        request.setRealName("测试用户");
        request.setStudentId("20260001");
        request.setCollege("计算机学院");
        return request;
    }
}
