package com.lihuahua.hyperspace.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


@Component
public class JwtTokenUtil  {

    private static int expiration;
    private static long longExpiration;
    
    // 用于存储安全密钥
    private static SecretKey signingKey;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        // 检查是否应该使用配置的密钥还是生成新的安全密钥
        if ("GENERATE_SECURE_KEY".equals(secret)) {
            // 使用JWT库推荐的方法生成适合HS512的安全密钥
            JwtTokenUtil.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        } else {
            // 使用配置文件中的密钥
            JwtTokenUtil.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        }
    }

    @Value("${jwt.expiration}")
    public void setExpiration(int expiration) {
        JwtTokenUtil.expiration = expiration;
    }

    @Value("${jwt.long-expiration:604800}")
    public void setLongExpiration(long longExpiration) {
        JwtTokenUtil.longExpiration = longExpiration;
    }

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    /**
     * 根据用户ID生成短期JWT令牌
     * @param userId 用户ID
     * @return 生成的JWT令牌字符串
     */
    public static String generateShortToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userId, expiration);
    }
    
    /**
     * 根据用户ID生成长期JWT令牌
     * @param userId 用户ID
     * @return 生成的JWT令牌字符串
     */
    public static String generateLongToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userId, longExpiration);
    }

    /**
     * 创建JWT令牌（支持自定义过期时间）
     * @param claims 自定义声明
     * @param subject 主题（用户ID）
     * @param expirationTime 过期时间（秒）
     * @return 生成的JWT令牌字符串
     */
    private static String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        long currentTimeMillis = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(currentTimeMillis))
                .setExpiration(new Date(currentTimeMillis + expirationTime * 1000L))
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public static String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public static Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT validation failed: " + e.getMessage());
        }
        return false;
    }

    private String refreshToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return createToken(claims, claims.getSubject(), expiration);
    }
    
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 获取当前使用的签名密钥（主要用于测试）
     * @return 当前签名密钥
     */
    public static SecretKey getSigningKey() {
        return signingKey;
    }
}