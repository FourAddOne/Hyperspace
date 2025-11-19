package com.lihuahua.hyperspace.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.TimeUnit;


@Component
public class JwtTokenUtil  {

    private static int expiration;
    private static long longExpiration;
    
    // 用于存储安全密钥
    private static SecretKey signingKey;
    
    // 注入RedisTemplate用于存储token
    private static RedisTemplate<String, String> redisTemplate;
    
    // Token在Redis中的保存时间（12小时）
    private static final long TOKEN_EXPIRATION_HOURS = 12;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        // 检查是否应该使用配置的密钥还是生成新的安全密钥
        if ("GENERATE_SECURE_KEY".equals(secret)) {
            // 使用JWT库推荐的方法生成适合HS512的安全密钥
            JwtTokenUtil.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            logger.info("使用自动生成的HS512安全密钥");
        } else {
            // 使用配置文件中的密钥
            JwtTokenUtil.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
            logger.info("使用配置文件中的密钥，长度: {} 字节", secret.getBytes().length);
        }
        
        // 检查密钥长度是否符合HS512要求（至少512位，即64字节）
        if (JwtTokenUtil.signingKey.getEncoded().length < 64) {
            logger.warn("警告：JWT签名密钥长度不足512位，当前长度: {} 字节", JwtTokenUtil.signingKey.getEncoded().length);
        } else {
            logger.info("JWT签名密钥长度符合要求: {} 字节", JwtTokenUtil.signingKey.getEncoded().length);
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
    
    // 注入RedisTemplate
    @Autowired
    public void setRedisTemplate(RedisTemplate<String, String> customStringRedisTemplate) {
        JwtTokenUtil.redisTemplate = customStringRedisTemplate;
    }

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    /**
     * 根据用户ID生成短期JWT令牌
     * @param userId 用户ID
     * @return 生成的JWT令牌字符串
     */
    public static String generateAccessToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userId, expiration);
    }
    
    /**
     * 根据用户ID生成长期JWT令牌
     * @param userId 用户ID
     * @return 生成的JWT令牌字符串
     */
    public static String generateRefreshToken(String userId) {
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
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(currentTimeMillis))
                .setExpiration(new Date(currentTimeMillis + expirationTime * 1000L))
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
        
        // 将token保存到Redis中，有效期12小时
        saveTokenToRedis(subject, token);
        
        return token;
    }
    
    /**
     * 将token保存到Redis中
     * @param userId 用户ID
     * @param token JWT令牌
     */
    private static void saveTokenToRedis(String userId, String token) {
        if (redisTemplate != null) {
            // 以"token:user_id"为key，token为value保存到Redis
            String key = "token:" + userId;
            redisTemplate.opsForValue().set(key, token, TOKEN_EXPIRATION_HOURS, TimeUnit.HOURS);
        }
    }
    
    /**
     * 从Redis中删除用户的token
     * @param userId 用户ID
     */
    public static void removeTokenFromRedis(String userId) {
        if (redisTemplate != null) {
            String key = "token:" + userId;
            redisTemplate.delete(key);
        }
    }
    
    /**
     * 验证token是否有效且存在于Redis中
     * @param token JWT令牌
     * @return token是否有效
     */
    public static String validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT validation failed: " + e.getMessage());
        }
        return null;
    }
    
    public static String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    
    /**
     * 获取当前使用的签名密钥（主要用于测试）
     * @return 当前签名密钥
     */
    public static SecretKey getSigningKey() {
        return signingKey;
    }
    
    /**
     * 检查当前签名密钥是否符合HS512算法的安全要求
     * @return 如果密钥长度符合要求返回true，否则返回false
     */
    public static boolean isSigningKeySecure() {
        if (signingKey == null) {
            return false;
        }
        // HS512算法要求密钥至少512位（64字节）
        return signingKey.getEncoded().length >= 64;
    }
    
    /**
     * 获取当前线程中处理请求的用户ID
     * @return 当前用户ID
     */
    public static String getCurrentUserId() {
        // 从Security上下文中获取当前认证的用户名（即用户ID）
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }
}