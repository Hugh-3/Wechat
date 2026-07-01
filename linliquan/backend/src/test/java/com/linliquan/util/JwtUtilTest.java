package com.linliquan.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil单元测试
 * 测试JWT Token生成、校验、解析功能
 */
@DisplayName("JwtUtil单元测试")
class JwtUtilTest {

    private static final Long TEST_USER_ID = 12345L;
    private static final String TEST_PHONE_HASH = "abc123def456";

    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH);
    }

    // ==================== Token生成测试 ====================

    @Test
    @DisplayName("测试generateToken - 基本生成")
    void testGenerateToken() {
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT格式: header.payload.signature
    }

    @Test
    @DisplayName("测试generateToken - 不同用户生成不同Token")
    void testGenerateToken_DifferentUsers() {
        String token1 = JwtUtil.generateToken(1L, "phone1");
        String token2 = JwtUtil.generateToken(2L, "phone2");

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("测试generateToken - 相同用户相同参数生成不同Token（时间戳不同）")
    void testGenerateToken_SameUserDifferentToken() {
        String token1 = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH);
        try {
            Thread.sleep(10); // 等待10ms确保时间戳不同
        } catch (InterruptedException e) {
            // ignore
        }
        String token2 = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH);

        // 时间戳不同导致createdAt不同
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("测试generateTokenWithCustomExpire - 自定义过期时间")
    void testGenerateTokenWithCustomExpire() {
        String shortToken = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH, 3600L); // 1小时
        String longToken = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH, 86400L); // 24小时

        assertNotNull(shortToken);
        assertNotNull(longToken);

        // 验证过期时间不同
        LocalDateTime shortExpire = JwtUtil.getExpireTimeFromToken(shortToken);
        LocalDateTime longExpire = JwtUtil.getExpireTimeFromToken(longToken);

        long diffMinutes = ChronoUnit.MINUTES.between(shortExpire, longExpire);
        assertTrue(diffMinutes >= 1380); // 约23小时
    }

    @Test
    @DisplayName("测试generateToken - 边界值用户ID")
    void testGenerateToken_BoundaryUserId() {
        // 最小值
        String minIdToken = JwtUtil.generateToken(0L, TEST_PHONE_HASH);
        assertTrue(JwtUtil.isTokenValid(minIdToken));

        // 最大值（Long.MAX_VALUE）
        String maxIdToken = JwtUtil.generateToken(Long.MAX_VALUE, TEST_PHONE_HASH);
        assertTrue(JwtUtil.isTokenValid(maxIdToken));
    }

    // ==================== Token校验测试 ====================

    @Test
    @DisplayName("测试verifyToken - 有效Token")
    void testVerifyToken_Valid() {
        DecodedJWT decoded = JwtUtil.verifyToken(validToken);

        assertNotNull(decoded);
        assertEquals(TEST_USER_ID, decoded.getClaim("userId").asLong());
        assertEquals(TEST_PHONE_HASH, decoded.getClaim("phoneHash").asString());
    }

    @Test
    @DisplayName("测试verifyToken - 无效Token格式")
    void testVerifyToken_InvalidFormat() {
        assertThrows(JWTVerificationException.class, () ->
            JwtUtil.verifyToken("invalid.token.format")
        );
    }

    @Test
    @DisplayName("测试verifyToken - 空Token")
    void testVerifyToken_EmptyToken() {
        assertThrows(JWTVerificationException.class, () ->
            JwtUtil.verifyToken("")
        );
    }

    @Test
    @DisplayName("测试verifyToken - null Token")
    void testVerifyToken_NullToken() {
        assertThrows(JWTVerificationException.class, () ->
            JwtUtil.verifyToken(null)
        );
    }

    @Test
    @DisplayName("测试verifyToken - 被篡改的Token")
    void testVerifyToken_TamperedToken() {
        String tampered = validToken.substring(0, validToken.length() - 5) + "xxxxx";
        assertThrows(JWTVerificationException.class, () ->
            JwtUtil.verifyToken(tampered)
        );
    }

    @Test
    @DisplayName("测试verifyToken - 使用错误密钥签名的Token")
    void testVerifyToken_WrongSignature() {
        // 用不同密钥生成Token
        String wrongKeyToken = JWT.create()
            .withClaim("userId", TEST_USER_ID)
            .withClaim("phoneHash", TEST_PHONE_HASH)
            .withExpiresAt(new Date(System.currentTimeMillis() + 86400000))
            .sign(Algorithm.HMAC256("wrong_secret_key"));

        assertThrows(JWTVerificationException.class, () ->
            JwtUtil.verifyToken(wrongKeyToken)
        );
    }

    @Test
    @DisplayName("测试verifyToken - 已过期的Token")
    void testVerifyToken_ExpiredToken() {
        // 生成1秒后过期的Token
        String expiredToken = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH, 0L);
        
        // 等待Token过期
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            // ignore
        }

        assertThrows(JWTVerificationException.class, () ->
            JwtUtil.verifyToken(expiredToken)
        );
    }

    @Test
    @DisplayName("测试isTokenValid - 有效Token")
    void testIsTokenValid_Valid() {
        assertTrue(JwtUtil.isTokenValid(validToken));
    }

    @Test
    @DisplayName("测试isTokenValid - 无效Token")
    void testIsTokenValid_Invalid() {
        assertFalse(JwtUtil.isTokenValid("invalid.token"));
        assertFalse(JwtUtil.isTokenValid(""));
        assertFalse(JwtUtil.isTokenValid(null));
    }

    // ==================== Token解析测试 ====================

    @Test
    @DisplayName("测试getUserIdFromToken - 正确提取用户ID")
    void testGetUserIdFromToken() {
        Long userId = JwtUtil.getUserIdFromToken(validToken);
        assertEquals(TEST_USER_ID, userId);
    }

    @Test
    @DisplayName("测试getPhoneHashFromToken - 正确提取手机号哈希")
    void testGetPhoneHashFromToken() {
        String phoneHash = JwtUtil.getPhoneHashFromToken(validToken);
        assertEquals(TEST_PHONE_HASH, phoneHash);
    }

    @Test
    @DisplayName("测试getCreatedAtFromToken - 正确提取创建时间")
    void testGetCreatedAtFromToken() {
        LocalDateTime createdAt = JwtUtil.getCreatedAtFromToken(validToken);
        
        assertNotNull(createdAt);
        // 创建时间应该在当前时间之前不久
        LocalDateTime now = LocalDateTime.now();
        assertTrue(createdAt.isBefore(now) || createdAt.isEqual(now));
        assertTrue(createdAt.isAfter(now.minusMinutes(1)));
    }

    @Test
    @DisplayName("测试getExpireTimeFromToken - 正确提取过期时间")
    void testGetExpireTimeFromToken() {
        LocalDateTime expireTime = JwtUtil.getExpireTimeFromToken(validToken);
        
        assertNotNull(expireTime);
        // 过期时间应该是24小时后
        LocalDateTime now = LocalDateTime.now();
        assertTrue(expireTime.isAfter(now.plusHours(23)));
        assertTrue(expireTime.isBefore(now.plusHours(25)));
    }

    @Test
    @DisplayName("测试getRemainingSeconds - 有效Token")
    void testGetRemainingSeconds() {
        long remaining = JwtUtil.getRemainingSeconds(validToken);
        
        assertTrue(remaining > 0);
        assertTrue(remaining <= 86400); // 不超过24小时
    }

    @Test
    @DisplayName("测试getRemainingSeconds - 无效Token")
    void testGetRemainingSeconds_InvalidToken() {
        assertEquals(0, JwtUtil.getRemainingSeconds("invalid"));
    }

    @Test
    @DisplayName("测试isTokenExpiringSoon - Token即将过期")
    void testIsTokenExpiringSoon_True() {
        // 生成1分钟过期的Token
        String shortToken = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH, 30L);
        
        assertTrue(JwtUtil.isTokenExpiringSoon(shortToken, 60)); // 60秒阈值
    }

    @Test
    @DisplayName("测试isTokenExpiringSoon - Token未即将过期")
    void testIsTokenExpiringSoon_False() {
        assertFalse(JwtUtil.isTokenExpiringSoon(validToken, 60)); // 24小时Token不会在60秒内过期
    }

    @Test
    @DisplayName("测试decodeToken - 仅解析不解密")
    void testDecodeToken() {
        DecodedJWT decoded = JwtUtil.decodeToken(validToken);
        
        assertNotNull(decoded);
        assertEquals(TEST_USER_ID, decoded.getClaim("userId").asLong());
    }

    @Test
    @DisplayName("测试decodeToken - 解析被篡改的Token（不解码会成功但解码后无法验签）")
    void testDecodeToken_Tampered() {
        String tampered = validToken.substring(0, validToken.length() - 5) + "xxxxx";
        DecodedJWT decoded = JwtUtil.decodeToken(tampered);
        
        assertNotNull(decoded);
        // 解码成功但验签会失败
        assertThrows(JWTVerificationException.class, () ->
            JwtUtil.verifyToken(tampered)
        );
    }

    // ==================== 集成场景测试 ====================

    @Test
    @DisplayName("场景测试 - 完整登录Token流程")
    void testLoginTokenFlow() {
        // 1. 用户登录，生成Token
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH);
        assertNotNull(token);

        // 2. 验证Token有效性
        assertTrue(JwtUtil.isTokenValid(token));

        // 3. 解析用户信息
        Long userId = JwtUtil.getUserIdFromToken(token);
        String phoneHash = JwtUtil.getPhoneHashFromToken(token);
        assertEquals(TEST_USER_ID, userId);
        assertEquals(TEST_PHONE_HASH, phoneHash);

        // 4. 检查Token剩余有效期
        long remaining = JwtUtil.getRemainingSeconds(token);
        assertTrue(remaining > 0);

        // 5. 模拟请求间隔后再次验证
        assertTrue(JwtUtil.isTokenValid(token)); // Token仍然有效
    }

    @Test
    @DisplayName("场景测试 - Token刷新判断")
    void testTokenRefreshScenario() {
        // 1. 生成Token
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH);
        
        // 2. 24小时Token不需要刷新
        assertFalse(JwtUtil.isTokenExpiringSoon(token, 3600)); // 1小时阈值

        // 3. 生成短期Token需要刷新
        String shortToken = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH, 1800L); // 30分钟
        assertTrue(JwtUtil.isTokenExpiringSoon(shortToken, 3600)); // 1小时阈值，30分钟Token需要刷新
    }

    @Test
    @DisplayName("场景测试 - 多用户并发Token生成")
    void testMultipleUsersTokenGeneration() {
        // 模拟100个用户同时获取Token
        String[] tokens = new String[100];
        for (int i = 0; i < 100; i++) {
            tokens[i] = JwtUtil.generateToken((long) i, "phone_" + i);
        }

        // 验证所有Token都有效且唯一
        for (int i = 0; i < 100; i++) {
            assertTrue(JwtUtil.isTokenValid(tokens[i]));
            assertEquals((long) i, JwtUtil.getUserIdFromToken(tokens[i]));
        }

        // 验证Token唯一性
        long uniqueTokenCount = java.util.Arrays.stream(tokens).distinct().count();
        assertEquals(100, uniqueTokenCount);
    }

    @Test
    @DisplayName("性能测试 - Token生成性能")
    void testTokenGenerationPerformance() {
        long startTime = System.currentTimeMillis();
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 1000次生成应该在1秒内完成
        assertTrue(duration < 1000, "Token generation too slow: " + duration + "ms for " + iterations + " iterations");
    }

    @Test
    @DisplayName("性能测试 - Token验证性能")
    void testTokenVerificationPerformance() {
        String token = JwtUtil.generateToken(TEST_USER_ID, TEST_PHONE_HASH);
        long startTime = System.currentTimeMillis();
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            JwtUtil.isTokenValid(token);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 1000次验证应该在1秒内完成
        assertTrue(duration < 1000, "Token verification too slow: " + duration + "ms for " + iterations + " iterations");
    }

    @Test
    @DisplayName("边界测试 - 特殊字符手机号哈希")
    void testSpecialCharactersPhoneHash() {
        String[] specialHashes = {
            "abc123!@#$%^&*()",
            "中文哈希值测试",
            "emoji🎉test",
            "spaces and\ttabs",
            "very_long_hash_".repeat(50)
        };

        for (String hash : specialHashes) {
            String token = JwtUtil.generateToken(TEST_USER_ID, hash);
            assertTrue(JwtUtil.isTokenValid(token));
            assertEquals(hash, JwtUtil.getPhoneHashFromToken(token));
        }
    }
}
