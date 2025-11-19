package com.lihuahua.hyperspace.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
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


@Component
public class JwtTokenUtil  {

    private static int expiration;
    private static long longExpiration;


    private static RedisTemplate<String, Object> redisTemplate;

    /**
     * -- GETTER --
     *  获取当前使用的签名密钥（主要用于测试）
     *
     * @return 当前签名密钥
     */
    // 用于存储安全密钥
    @Getter
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

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        JwtTokenUtil.redisTemplate = redisTemplate;
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

    /**
     * 验证JWT令牌的有效性
     * @param token 需要验证的JWT令牌字符串
     * @return 如果令牌有效返回true，否则返回false
     */
    public static Boolean validateToken(String token) {
        try {
            // 首先检查token是否在黑名单中（已注销）
            if (redisTemplate.hasKey("blacklist:" + token)) {
                logger.info("Token在黑名单中，已注销");
                return false;
            }
            
            // 验证JWT本身的签名和有效期
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            
            return true; // 如果解析成功，则令牌有效
        } catch (JwtException | IllegalArgumentException e) {
            // 捕获JWT异常或非法参数异常，记录错误日志
            logger.error("JWT validation failed: " + e.getMessage());
        }
        return false; // 如果解析过程中出现异常，则令牌无效
    }

    /**
     * 将token加入黑名单（注销token）
     * @param token 要注销的token
     */
    public static void blacklistToken(String token) {
        try {
            // 解析token获取过期时间
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Date expirationDate = claims.getExpiration();
            long ttl = expirationDate.getTime() - System.currentTimeMillis();
            
            // 将token加入黑名单，设置与token相同的过期时间
            if (ttl > 0) {
                redisTemplate.opsForValue().set("blacklist:" + token, "true", ttl, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (JwtException e) {
            logger.error("无法解析token以获取过期时间: " + e.getMessage());
        }
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

}