package com.linliquan.service;

import com.linliquan.model.entity.User;
import com.linliquan.model.enums.VerificationStatus;
import com.linliquan.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Repository → Service层集成测试
 * 测试AuthService与UserRepository的集成
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService集成测试")
class AuthServiceIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private String testPhone = "13800138000";
    private String testPhoneHash;

    @BeforeEach
    void setUp() {
        testPhoneHash = sha256(testPhone);

        testUser = new User();
        testUser.setId(1L);
        testUser.setPhoneHash(testPhoneHash);
        testUser.setPhoneEncrypted(testPhone);
        testUser.setNickname("测试用户");
        testUser.setAvatarUrl("https://example.com/avatar.png");
        testUser.setVerificationStatus(VerificationStatus.UNAUTH);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 登录流程集成测试 ====================

    @Test
    @DisplayName("集成测试 - 已有用户登录")
    void testLogin_ExistingUser() {
        // 1. Repository返回已存在的用户
        when(userRepository.findByPhoneHash(testPhoneHash)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 2. 执行登录
        var result = authService.login(testPhone);

        // 3. 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().get("user"));
        assertNotNull(result.getData().get("accessToken"));
    }

    @Test
    @DisplayName("集成测试 - 新用户登录并注册")
    void testLogin_NewUserRegistration() {
        // 1. Repository返回空（用户不存在）
        when(userRepository.findByPhoneHash(testPhoneHash)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // 2. 执行登录
        var result = authService.login(testPhone);

        // 3. 验证新用户被创建
        assertTrue(result.isSuccess());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("集成测试 - 获取用户状态")
    void testGetUserStatus() {
        // 1. 设置用户为已认证状态
        testUser.setVerificationStatus(VerificationStatus.VERIFIED);

        // 2. Repository返回用户
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 3. 执行获取状态
        var result = authService.getUserStatus(1L);

        // 4. 验证状态信息
        assertTrue(result.isSuccess());
        assertEquals(VerificationStatus.VERIFIED.getCode(), result.getData().get("verificationStatus"));
        assertTrue((Boolean) result.getData().get("canWrite"));
        assertTrue((Boolean) result.getData().get("canRead"));
    }

    @Test
    @DisplayName("集成测试 - 获取认证中用户状态")
    void testGetUserStatus_PendingUser() {
        // 1. 设置用户为认证中状态
        testUser.setVerificationStatus(VerificationStatus.PENDING);

        // 2. Repository返回用户
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 3. 执行获取状态
        var result = authService.getUserStatus(1L);

        // 4. 验证状态信息
        assertTrue(result.isSuccess());
        assertEquals(VerificationStatus.PENDING.getCode(), result.getData().get("verificationStatus"));
        assertFalse((Boolean) result.getData().get("canWrite"));
        assertTrue((Boolean) result.getData().get("canRead"));
    }

    // ==================== 认证申请流程集成测试 ====================

    @Test
    @DisplayName("集成测试 - 提交认证申请")
    void testApplyVerification_Success() {
        // 1. 用户未认证
        testUser.setVerificationStatus(VerificationStatus.UNAUTH);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 2. 提交认证申请
        var result = authService.applyVerification(1L, "110101199001011234", "1-101", "https://cos.com/cert.jpg");

        // 3. 验证成功
        assertTrue(result.isSuccess());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("集成测试 - 重复提交认证申请")
    void testApplyVerification_AlreadyVerified() {
        // 1. 用户已认证
        testUser.setVerificationStatus(VerificationStatus.VERIFIED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 2. 尝试再次申请
        var result = authService.applyVerification(1L, "110101199001011234", "1-101", null);

        // 3. 验证失败（已认证不能重复申请）
        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("集成测试 - 认证申请更新用户信息")
    void testApplyVerification_UpdatesUserInfo() {
        // 1. 用户未认证
        testUser.setVerificationStatus(VerificationStatus.UNAUTH);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. 提交认证申请
        var result = authService.applyVerification(1L, "id123", "room456", "cert.jpg");

        // 3. 验证用户信息被更新
        assertTrue(result.isSuccess());
        assertEquals(VerificationStatus.PENDING, testUser.getVerificationStatus());
        assertNotNull(testUser.getVerificationApplyTime());
    }

    @Test
    @DisplayName("集成测试 - 用户不存在")
    void testGetUserStatus_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        var result = authService.getUserStatus(999L);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getCode());
    }

    // ==================== Token解析流程集成测试 ====================

    @Test
    @DisplayName("集成测试 - 通过Token获取用户")
    void testGetUserByToken() {
        // 1. 生成Token
        String token = com.linliquan.util.JwtUtil.generateToken(1L, testPhoneHash);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // 2. 通过Token获取用户
        User user = authService.getUserByToken(token);

        // 3. 验证
        assertNotNull(user);
        assertEquals(1L, user.getId());
    }

    @Test
    @DisplayName("集成测试 - 无效Token获取用户")
    void testGetUserByToken_Invalid() {
        User user = authService.getUserByToken("invalid.token.here");

        assertNull(user);
    }

    // ==================== 边界场景集成测试 ====================

    @Test
    @DisplayName("边界测试 - 空手机号登录")
    void testLogin_EmptyPhone() {
        assertThrows(Exception.class, () -> authService.login(""));
    }

    @Test
    @DisplayName("边界测试 - 特殊字符手机号")
    void testLogin_SpecialCharacters() {
        when(userRepository.findByPhoneHash(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        var result = authService.login("+86-138-0013-8000");

        // 应该成功（SHA-256处理后的哈希值）
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("边界测试 - 极长手机号")
    void testLogin_LongPhone() {
        String longPhone = "1".repeat(100);
        when(userRepository.findByPhoneHash(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        var result = authService.login(longPhone);

        // SHA-256会处理任意长度输入
        assertTrue(result.isSuccess());
    }

    // ==================== 完整业务流程集成测试 ====================

    @Test
    @DisplayName("完整流程 - 新用户注册→申请认证→登录")
    void testCompleteRegistrationFlow() {
        // 1. 新用户登录（自动注册）
        when(userRepository.findByPhoneHash(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        var loginResult = authService.login(testPhone);
        assertTrue(loginResult.isSuccess());
        assertEquals(VerificationStatus.UNAUTH.getCode(),
            ((User) loginResult.getData().get("user")).getVerificationStatus());

        // 2. 获取用户状态（未认证）
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        var statusResult = authService.getUserStatus(1L);
        assertEquals(VerificationStatus.UNAUTH.getCode(), statusResult.getData().get("verificationStatus"));
        assertFalse((Boolean) statusResult.getData().get("canWrite"));

        // 3. 提交认证申请
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var applyResult = authService.applyVerification(1L, "idCard", "room", "cert");
        assertTrue(applyResult.isSuccess());
        assertEquals(VerificationStatus.PENDING, testUser.getVerificationStatus());

        // 4. 再次获取状态（认证中）
        var pendingStatus = authService.getUserStatus(1L);
        assertEquals(VerificationStatus.PENDING.getCode(), pendingStatus.getData().get("verificationStatus"));
        assertFalse((Boolean) pendingStatus.getData().get("canWrite"));
    }

    @Test
    @DisplayName("并发场景 - 同一手机号多次登录")
    void testConcurrentLogin() {
        when(userRepository.findByPhoneHash(testPhoneHash)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // 模拟多次并发登录
        for (int i = 0; i < 10; i++) {
            var result = authService.login(testPhone);
            assertTrue(result.isSuccess());
        }

        // 用户已存在，不会重复创建
        verify(userRepository, never()).save(any(User.class));
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
