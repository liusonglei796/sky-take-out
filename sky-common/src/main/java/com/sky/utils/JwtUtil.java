package com.sky.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类（jjwt 0.12.x）
 */
@Component
public class JwtUtil {

    /**
     * 生成 JWT（HS256）
     *
     * @param secretKey jwt 秘钥（建议长度 ≥ 32 字节）
     * @param ttlMillis jwt 过期时间（毫秒）
     * @param claims    自定义载荷
     * @return JWT 字符串
     */
    public static String createJWT(String secretKey, long ttlMillis, Map<String, Object> claims) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Date exp = new Date(System.currentTimeMillis() + ttlMillis);

        return Jwts.builder()
                .claims(claims)
                .expiration(exp)
                .signWith(key)   // jjwt 0.12.x 自动选择 HS256/HS384/HS512
                .compact();
    }

    /**
     * 解析 JWT，返回载荷 Claims
     *
     * @param secretKey jwt 秘钥
     * @param token     JWT 字符串
     * @return Claims
     */
    public static Claims parseJWT(String secretKey, String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
