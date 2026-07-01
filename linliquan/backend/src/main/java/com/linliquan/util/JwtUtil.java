package com.linliquan.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * JWT工具类
 * 【安全要求】Token有效期24小时，防篡改校验
 */
public class JwtUtil {

    private static final String SECRET_KEY = "linliquan_secret_key_2026";
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 86400L; // 24小时
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_PHONE_HASH = "phoneHash";
    private static final String CLAIM_CREATED_AT = "createdAt";

    /**
     * 生成Token
     */
    public static String generateToken(Long userId, String phoneHash) {
        Date expireDate = Date.from(
            LocalDateTime.now().plusSeconds(ACCESS_TOKEN_EXPIRE_SECONDS)
                .atZone(ZoneId.systemDefault()).toInstant()
        );

        return JWT.create()
                .withClaim(CLAIM_USER_ID, userId)
                .withClaim(CLAIM_PHONE_HASH, phoneHash)
                .withClaim(CLAIM_CREATED_AT, System.currentTimeMillis())
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    /**
     * 生成带自定义过期时间的Token
     */
    public static String generateToken(Long userId, String phoneHash, long expireSeconds) {
        Date expireDate = Date.from(
            LocalDateTime.now().plusSeconds(expireSeconds)
                .atZone(ZoneId.systemDefault()).toInstant()
        );

        return JWT.create()
                .withClaim(CLAIM_USER_ID, userId)
                .withClaim(CLAIM_PHONE_HASH, phoneHash)
                .withClaim(CLAIM_CREATED_AT, System.currentTimeMillis())
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    /**
     * 校验Token合法性
     * @throws JWTVerificationException Token无效或已过期
     */
    public static DecodedJWT verifyToken(String token) throws JWTVerificationException {
        return JWT.require(Algorithm.HMAC256(SECRET_KEY))
                .build()
                .verify(token);
    }

    /**
     * 校验Token合法性，返回是否有效（不抛异常）
     */
    public static boolean isTokenValid(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /**
     * 从Token中提取用户ID
     */
    public static Long getUserIdFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim(CLAIM_USER_ID).asLong();
    }

    /**
     * 从Token中提取手机号哈希
     */
    public static String getPhoneHashFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim(CLAIM_PHONE_HASH).asString();
    }

    /**
     * 从Token中提取创建时间
     */
    public static LocalDateTime getCreatedAtFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        long timestamp = jwt.getClaim(CLAIM_CREATED_AT).asLong();
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        );
    }

    /**
     * 获取Token过期时间
     */
    public static LocalDateTime getExpireTimeFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        Date expireDate = jwt.getExpiresAt();
        return LocalDateTime.ofInstant(
            expireDate.toInstant(),
            ZoneId.systemDefault()
        );
    }

    /**
     * 计算Token剩余有效时间（秒）
     */
    public static long getRemainingSeconds(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            Date expireDate = jwt.getExpiresAt();
            long remaining = (expireDate.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (JWTVerificationException e) {
            return 0;
        }
    }

    /**
     * 检查Token是否即将过期（剩余时间小于阈值）
     */
    public static boolean isTokenExpiringSoon(String token, long thresholdSeconds) {
        return getRemainingSeconds(token) < thresholdSeconds;
    }

    /**
     * 解析Token（不校验，用于获取基本信息）
     */
    public static DecodedJWT decodeToken(String token) {
        return JWT.decode(token);
    }

    private static final String CLAIM_IS_ADMIN = "isAdmin";
    private static final String CLAIM_ADMIN_ID = "adminId";
    private static final String CLAIM_ADMIN_USERNAME = "adminUsername";

    /**
     * 生成管理员Token
     */
    public static String generateAdminToken(Long adminId, String username) {
        Date expireDate = Date.from(
            LocalDateTime.now().plusSeconds(ACCESS_TOKEN_EXPIRE_SECONDS)
                .atZone(ZoneId.systemDefault()).toInstant()
        );
        return JWT.create()
                .withClaim(CLAIM_ADMIN_ID, adminId)
                .withClaim(CLAIM_ADMIN_USERNAME, username)
                .withClaim(CLAIM_IS_ADMIN, true)
                .withClaim(CLAIM_CREATED_AT, System.currentTimeMillis())
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    /**
     * 从Token中提取管理员ID
     */
    public static Long getAdminIdFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim(CLAIM_ADMIN_ID).asLong();
    }

    /**
     * 判断是否为管理员Token
     */
    public static boolean isAdminToken(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            Boolean isAdmin = jwt.getClaim(CLAIM_IS_ADMIN).asBoolean();
            return isAdmin != null && isAdmin;
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
