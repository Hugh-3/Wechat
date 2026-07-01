package com.linliquan.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET_KEY = "linliquan_secret_key_2026";
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 86400L;

    public static String generateToken(Long userId, String phoneHash) {
        Date expireDate = Date.from(
            LocalDateTime.now().plusSeconds(ACCESS_TOKEN_EXPIRE_SECONDS)
                .atZone(ZoneId.systemDefault()).toInstant()
        );

        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("phoneHash", phoneHash)
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    public static DecodedJWT verifyToken(String token) throws JWTVerificationException {
        return JWT.require(Algorithm.HMAC256(SECRET_KEY))
                .build()
                .verify(token);
    }

    public static Long getUserIdFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("userId").asLong();
    }

    public static String getPhoneHashFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("phoneHash").asString();
    }
}
