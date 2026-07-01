package com.linliquan.interceptor;

import com.linliquan.model.entity.AdminUser;
import com.linliquan.repository.AdminUserRepository;
import com.linliquan.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminInterceptor 单元测试
 * 测试管理员拦截器对不同 Token 场景的处理
 * - 无 Authorization 头
 * - 非管理员 Token（普通用户 Token）
 * - 有效管理员 Token
 * - 已禁用管理员
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminInterceptor单元测试")
class AdminInterceptorTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AdminInterceptor adminInterceptor;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    /**
     * 测试用例1：无 Authorization 头 → 返回 false 并发送 403
     */
    @Test
    @DisplayName("无Authorization头 - 返回false并发送403")
    void testPreHandle_NoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        boolean result = adminInterceptor.preHandle(request, response, new Object());

        assertFalse(result, "无 Authorization 头应返回 false");
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(responseWriter.toString().contains("\"success\":false"));
    }

    /**
     * 测试用例2：非管理员 Token（普通用户 Token）→ 返回 false 并发送 403
     */
    @Test
    @DisplayName("非管理员Token - 返回false并发送403")
    void testPreHandle_NonAdminToken() throws Exception {
        // 生成普通用户 Token（不含 isAdmin 声明）
        String userToken = JwtUtil.generateToken(1L, "phone_hash");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + userToken);

        boolean result = adminInterceptor.preHandle(request, response, new Object());

        assertFalse(result, "非管理员 Token 应返回 false");
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(responseWriter.toString().contains("\"success\":false"));
    }

    /**
     * 测试用例3：有效管理员 Token 且管理员正常 → 返回 true 并设置 currentAdmin 属性
     */
    @Test
    @DisplayName("有效管理员Token - 返回true并设置currentAdmin属性")
    void testPreHandle_ValidAdminToken() throws Exception {
        String adminToken = JwtUtil.generateAdminToken(1L, "admin");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + adminToken);

        AdminUser admin = new AdminUser();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setNickname("测试管理员");
        admin.setRole(2);
        admin.setStatus(1);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin));

        boolean result = adminInterceptor.preHandle(request, response, new Object());

        assertTrue(result, "有效管理员 Token 应返回 true");
        verify(request).setAttribute(eq("currentAdmin"), any(AdminUser.class));
        verify(response, never()).setStatus(anyInt());
    }

    /**
     * 测试用例4：有效管理员 Token 但管理员已被禁用 → 返回 false 并发送 403
     */
    @Test
    @DisplayName("已禁用管理员 - 返回false并发送403")
    void testPreHandle_DisabledAdmin() throws Exception {
        String adminToken = JwtUtil.generateAdminToken(2L, "disabled_admin");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + adminToken);

        AdminUser disabledAdmin = new AdminUser();
        disabledAdmin.setId(2L);
        disabledAdmin.setUsername("disabled_admin");
        disabledAdmin.setStatus(2); // 禁用状态
        when(adminUserRepository.findById(2L)).thenReturn(Optional.of(disabledAdmin));

        boolean result = adminInterceptor.preHandle(request, response, new Object());

        assertFalse(result, "已禁用管理员应返回 false");
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(responseWriter.toString().contains("\"success\":false"));
    }
}
