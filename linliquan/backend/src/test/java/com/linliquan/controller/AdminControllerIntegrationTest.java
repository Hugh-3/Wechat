package com.linliquan.controller;

import com.linliquan.common.Result;
import com.linliquan.model.entity.AdminUser;
import com.linliquan.repository.AdminUserRepository;
import com.linliquan.service.AdminService;
import com.linliquan.service.CacheService;
import com.linliquan.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminController 接口集成测试
 * 使用 @SpringBootTest(RANDOM_PORT) + TestRestTemplate 测试完整的 HTTP 请求流程
 * 覆盖 AdminInterceptor 鉴权 + AdminController 路由 + AdminService 调用
 *
 * 【说明】
 * - 使用 JwtUtil.generateAdminToken(1L, "admin") 生成测试用管理员 Token
 * - @MockBean AdminService：隔离 Service 层，专注测试 HTTP 层
 * - @MockBean AdminUserRepository：拦截器需要查找管理员（RANDOM_PORT 使用独立线程，无法依赖 @Transactional 数据）
 * - @MockBean CacheService：避免 Redis 依赖
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("AdminController接口集成测试")
class AdminControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AdminUserRepository adminUserRepository;

    @MockBean
    private CacheService cacheService;

    private String baseUrl;
    private String adminToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/admin";

        // 生成管理员 Token（adminId=1, username="admin"）
        adminToken = JwtUtil.generateAdminToken(1L, "admin");

        // Mock AdminUserRepository：拦截器通过 findById(1L) 查找管理员
        AdminUser mockAdmin = new AdminUser();
        mockAdmin.setId(1L);
        mockAdmin.setUsername("admin");
        mockAdmin.setNickname("测试管理员");
        mockAdmin.setRole(2);
        mockAdmin.setStatus(1);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(mockAdmin));
    }

    // ==================== 登录接口测试 ====================

    /**
     * 测试用例1：POST /admin/login-mock（模拟登录） - 无需鉴权，返回 Token
     */
    @Test
    @DisplayName("POST /admin/login-mock - 模拟登录成功，返回Token")
    void testLoginMock_Success() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/login-mock?adminId=1&username=admin", null, String.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"success\":true"));
        assertTrue(response.getBody().contains("accessToken"));
    }

    // ==================== 拦截器鉴权测试 ====================

    /**
     * 测试用例2：GET /admin/dashboard 无 Token → 拦截器返回 403
     */
    @Test
    @DisplayName("GET /admin/dashboard 无Token - 拦截器返回403")
    void testDashboard_NoToken_Forbidden() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/dashboard", String.class);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"success\":false"));
    }

    /**
     * 测试用例3：GET /admin/dashboard 携带普通用户 Token（非管理员）→ 拦截器返回 403
     */
    @Test
    @DisplayName("GET /admin/dashboard 非管理员Token - 拦截器返回403")
    void testDashboard_NonAdminToken_Forbidden() {
        // 生成普通用户 Token（不含 isAdmin 声明）
        String userToken = JwtUtil.generateToken(1L, "phone_hash");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + userToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/dashboard", HttpMethod.GET, entity, String.class);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"success\":false"));
    }

    // ==================== 受保护接口测试（携带有效管理员 Token） ====================

    /**
     * 测试用例4：GET /admin/dashboard 携带有效管理员 Token → 返回统计数据
     */
    @Test
    @DisplayName("GET /admin/dashboard 有效管理员Token - 返回统计数据")
    void testDashboard_ValidAdminToken_Success() {
        // Mock AdminService.getStatistics() 返回统计结果
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("userStats", Map.of("total", 100));
        when(adminService.getStatistics()).thenReturn(Result.success(statsData));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/dashboard", HttpMethod.GET, entity, String.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"success\":true"));
    }

    /**
     * 测试用例5：GET /admin/verifications 携带有效管理员 Token → 返回认证列表
     */
    @Test
    @DisplayName("GET /admin/verifications 有效管理员Token - 返回认证列表")
    void testVerifications_WithAdminToken() {
        Map<String, Object> listData = new HashMap<>();
        listData.put("list", java.util.Collections.emptyList());
        listData.put("total", 0);
        when(adminService.getVerificationList(anyInt(), anyInt())).thenReturn(Result.success(listData));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/verifications?page=1&pageSize=20", HttpMethod.GET, entity, String.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"success\":true"));
    }

    /**
     * 测试用例6：GET /admin/users 携带有效管理员 Token → 返回用户列表
     */
    @Test
    @DisplayName("GET /admin/users 有效管理员Token - 返回用户列表")
    void testUserList_WithAdminToken() {
        Map<String, Object> listData = new HashMap<>();
        listData.put("list", java.util.Collections.emptyList());
        listData.put("total", 0);
        when(adminService.getUserList(anyInt(), anyInt(), any())).thenReturn(Result.success(listData));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/users?page=1&pageSize=20", HttpMethod.GET, entity, String.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"success\":true"));
    }

    /**
     * 测试用例7：POST /admin/users/{userId}/ban 携带有效管理员 Token → 封禁用户成功
     */
    @Test
    @DisplayName("POST /admin/users/{userId}/ban 有效管理员Token - 封禁成功")
    void testBanUser_WithAdminToken() {
        when(adminService.banUser(anyLong(), anyLong(), any())).thenReturn(Result.success(null));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("reason", "违规行为");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/users/100/ban", HttpMethod.POST, entity, String.class);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"success\":true"));
    }
}
