package com.linliquan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller → Service层集成测试
 * 测试AuthController与AuthService的集成
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController集成测试")
class AuthControllerIntegrationTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();

        testUser = new User();
        testUser.setId(1L);
        testUser.setPhoneHash("testHash");
        testUser.setNickname("测试用户");
        testUser.setVerificationStatus(VerificationStatus.VERIFIED);
    }

    // ==================== 登录接口测试 ====================

    @Test
    @DisplayName("集成测试 - 正常登录")
    void testLogin_Success() throws Exception {
        // 1. 准备Service响应
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("user", testUser);
        serviceResult.put("accessToken", "test_token_123");

        com.linliquan.common.Result<Map<String, Object>> result =
            com.linliquan.common.Result.success(serviceResult);

        when(authService.login(anyString())).thenReturn(result);

        // 2. 执行请求
        mockMvc.perform(post("/v1/auth/login")
                .param("phone", "13800138000")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("test_token_123"));

        // 3. 验证Service被调用
        verify(authService, times(1)).login("13800138000");
    }

    @Test
    @DisplayName("集成测试 - Mock登录接口")
    void testLoginMock_Success() throws Exception {
        mockMvc.perform(post("/v1/auth/login-mock")
                .param("statusType", "2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("mock_token_VERIFIED"))
            .andExpect(jsonPath("$.data.user.verificationStatus").value(2));
    }

    @Test
    @DisplayName("集成测试 - Mock登录不同状态")
    void testLoginMock_DifferentStatuses() throws Exception {
        // 测试未认证状态
        mockMvc.perform(post("/v1/auth/login-mock")
                .param("statusType", "0"))
            .andExpect(jsonPath("$.data.user.verificationStatus").value(0))
            .andExpect(jsonPath("$.data.user.verificationDesc").value("未认证"));

        // 测试认证中状态
        mockMvc.perform(post("/v1/auth/login-mock")
                .param("statusType", "1"))
            .andExpect(jsonPath("$.data.user.verificationStatus").value(1))
            .andExpect(jsonPath("$.data.user.verificationDesc").value("认证中"));

        // 测试已认证状态
        mockMvc.perform(post("/v1/auth/login-mock")
                .param("statusType", "2"))
            .andExpect(jsonPath("$.data.user.verificationStatus").value(2))
            .andExpect(jsonPath("$.data.user.verificationDesc").value("已认证"));
    }

    // ==================== 认证申请接口测试 ====================

    @Test
    @DisplayName("集成测试 - 提交认证申请")
    void testApplyVerification_Success() throws Exception {
        // 1. 准备请求
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("idCard", "110101199001011234");
        requestBody.put("houseNumber", "1-101");
        requestBody.put("certificateUrl", "https://cos.com/cert.jpg");

        // 2. 准备Service响应
        com.linliquan.common.Result<Void> serviceResult = com.linliquan.common.Result.success(null);
        when(authService.applyVerification(anyLong(), anyString(), anyString(), anyString()))
            .thenReturn(serviceResult);

        // 3. 执行请求（带认证用户）
        mockMvc.perform(post("/v1/auth/apply-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
                .requestAttr("currentUser", testUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // 4. 验证Service调用
        verify(authService, times(1)).applyVerification(eq(1L), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("集成测试 - 未登录用户申请认证")
    void testApplyVerification_Unauthorized() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("idCard", "110101199001011234");

        mockMvc.perform(post("/v1/auth/apply-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
                .requestAttr("currentUser", null)) // 未登录
            .andExpect(status().isUnauthorized());
    }

    // ==================== 状态查询接口测试 ====================

    @Test
    @DisplayName("集成测试 - 已登录用户获取状态")
    void testGetStatus_LoggedIn() throws Exception {
        // 1. 准备Service响应
        Map<String, Object> status = new HashMap<>();
        status.put("verificationStatus", VerificationStatus.VERIFIED.getCode());
        status.put("description", VerificationStatus.VERIFIED.getDescription());
        status.put("canWrite", true);
        status.put("canRead", true);

        com.linliquan.common.Result<Map<String, Object>> serviceResult =
            com.linliquan.common.Result.success(status);

        when(authService.getUserStatus(1L)).thenReturn(serviceResult);

        // 2. 执行请求
        mockMvc.perform(get("/v1/auth/status")
                .requestAttr("currentUser", testUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.verificationStatus").value(2))
            .andExpect(jsonPath("$.data.canWrite").value(true));
    }

    @Test
    @DisplayName("集成测试 - 未登录用户获取状态")
    void testGetStatus_NotLoggedIn() throws Exception {
        mockMvc.perform(get("/v1/auth/status")
                .requestAttr("currentUser", null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.verificationStatus").value(0))
            .andExpect(jsonPath("$.data.canWrite").value(false));
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("边界测试 - 空手机号")
    void testLogin_EmptyPhone() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                .param("phone", ""))
            .andExpect(status().isOk()); // Service层处理
    }

    @Test
    @DisplayName("边界测试 - 缺失必填参数")
    void testApplyVerification_MissingParams() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        // 缺少所有参数

        mockMvc.perform(post("/v1/auth/apply-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
                .requestAttr("currentUser", testUser))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("边界测试 - 非法状态类型")
    void testLoginMock_IllegalStatus() throws Exception {
        mockMvc.perform(post("/v1/auth/login-mock")
                .param("statusType", "99"))
            .andExpect(status().isOk());
    }

    // ==================== 异常场景测试 ====================

    @Test
    @DisplayName("异常测试 - Service返回错误")
    void testLogin_ServiceError() throws Exception {
        when(authService.login(anyString()))
            .thenReturn(com.linliquan.common.Result.fail(com.linliquan.common.ResultCode.INTERNAL_ERROR));

        mockMvc.perform(post("/v1/auth/login")
                .param("phone", "13800138000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("异常测试 - 用户不存在")
    void testGetStatus_UserNotFound() throws Exception {
        when(authService.getUserStatus(anyLong()))
            .thenReturn(com.linliquan.common.Result.fail(com.linliquan.common.ResultCode.NOT_FOUND));

        mockMvc.perform(get("/v1/auth/status")
                .requestAttr("currentUser", testUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 完整业务流程测试 ====================

    @Test
    @DisplayName("完整流程 - 用户登录→获取状态→申请认证")
    void testCompleteAuthFlow() throws Exception {
        // 1. 登录
        Map<String, Object> loginResult = new HashMap<>();
        loginResult.put("user", testUser);
        loginResult.put("accessToken", "token123");

        when(authService.login(anyString()))
            .thenReturn(com.linliquan.common.Result.success(loginResult));

        mockMvc.perform(post("/v1/auth/login")
                .param("phone", "13800138000"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists());

        // 2. 获取状态（已认证）
        Map<String, Object> status = new HashMap<>();
        status.put("verificationStatus", VerificationStatus.VERIFIED.getCode());
        status.put("description", "已认证");
        status.put("canWrite", true);
        status.put("canRead", true);

        when(authService.getUserStatus(1L))
            .thenReturn(com.linliquan.common.Result.success(status));

        mockMvc.perform(get("/v1/auth/status")
                .requestAttr("currentUser", testUser))
            .andExpect(jsonPath("$.data.canWrite").value(true));
    }

    @Test
    @DisplayName("完整流程 - 新用户注册→申请认证")
    void testNewUserRegistrationFlow() throws Exception {
        User newUser = new User();
        newUser.setId(2L);
        newUser.setVerificationStatus(VerificationStatus.UNAUTH);

        // 1. 新用户登录
        Map<String, Object> loginResult = new HashMap<>();
        loginResult.put("user", newUser);
        loginResult.put("accessToken", "new_token");

        when(authService.login(anyString()))
            .thenReturn(com.linliquan.common.Result.success(loginResult));

        mockMvc.perform(post("/v1/auth/login")
                .param("phone", "13900139000"))
            .andExpect(jsonPath("$.success").value(true));

        // 2. 提交认证申请
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("idCard", "id123");
        requestBody.put("houseNumber", "2-202");
        requestBody.put("certificateUrl", "cert.jpg");

        when(authService.applyVerification(anyLong(), anyString(), anyString(), anyString()))
            .thenReturn(com.linliquan.common.Result.success(null));

        mockMvc.perform(post("/v1/auth/apply-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
                .requestAttr("currentUser", newUser))
            .andExpect(jsonPath("$.success").value(true));
    }
}
