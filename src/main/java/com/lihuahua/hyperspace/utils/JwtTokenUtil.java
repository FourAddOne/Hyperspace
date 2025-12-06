package com.lihuahua.hyperspace.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Component
public class JwtTokenUtil  {

    private static int expiration;
    private static long longExpiration;
    
    // 用于存储安全密钥
    private static SecretKey signingKey;
    
    // 注入RedisTemplate用于存储token
    private static RedisTemplate<String, String> redisTemplate;
    
    // Token在Redis中的保存时间（小时）
    private static long tokenExpirationHours;
    
    // RefreshToken在Redis中的保存时间（天）
    private static long refreshTokenExpirationDays;

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
    
    @Value("${jwt.redis.token-expiration-hours:12}")
    public void setTokenExpirationHours(long tokenExpirationHours) {
        JwtTokenUtil.tokenExpirationHours = tokenExpirationHours;
    }
    
    @Value("${jwt.redis.refresh-token-expiration-days:14}")
    public void setRefreshTokenExpirationDays(long refreshTokenExpirationDays) {
        JwtTokenUtil.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }
    
    // 注入RedisTemplate
    @Autowired
    @Qualifier("redisTemplate")
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
        return createToken(claims, userId, expiration, false); // false表示是accessToken
    }
    
    /**
     * 根据用户ID生成长期JWT令牌
     * @param userId 用户ID
     * @return 生成的JWT令牌字符串
     */
    public static String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userId, longExpiration, true); // true表示是refreshToken
    }

    /**
     * 创建JWT令牌（支持自定义过期时间）
     * @param claims 自定义声明
     * @param subject 主题（用户ID）
     * @param expirationTime 过期时间（秒）
     * @param isRefreshToken 是否为刷新令牌
     * @return 生成的JWT令牌字符串
     */
    private static String createToken(Map<String, Object> claims, String subject, long expirationTime, boolean isRefreshToken) {
        long currentTimeMillis = System.currentTimeMillis();
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(currentTimeMillis))
                .setExpiration(new Date(currentTimeMillis + expirationTime * 1000)) // 修复此处的过期时间计算
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
        
        // 将token保存到Redis中
        saveTokenToRedis(subject, token, isRefreshToken);
        
        return token;
    }
    
    /**
     * 将token保存到Redis中
     * @param userId 用户ID
     * @param token JWT令牌
     * @param isRefreshToken 是否为刷新令牌
     */
    private static void saveTokenToRedis(String userId, String token, boolean isRefreshToken) {
        if (redisTemplate != null) {
            // 区分accessToken和refreshToken的存储key
            String key = (isRefreshToken ? "refresh_token:" : "access_token:") + userId;
            long expireTime = isRefreshToken ? refreshTokenExpirationDays * 24 : tokenExpirationHours;
            redisTemplate.opsForValue().set(key, token, expireTime, TimeUnit.HOURS);
            logger.debug("保存{}到Redis，过期时间:{}小时", isRefreshToken ? "refreshToken" : "accessToken", expireTime);
            logger.debug("用户{}的{}已保存到Redis，key: {}, 过期时间: {}小时", userId, isRefreshToken ? "refreshToken" : "accessToken", key, expireTime);
        } else {
            logger.warn("RedisTemplate为空，无法保存令牌到Redis");
        }
    }
    
    /**
     * 从Redis中删除用户的token
     * @param userId 用户ID
     */
    public static void removeTokenFromRedis(String userId) {
        if (redisTemplate != null) {
            String accessTokenKey = "access_token:" + userId;
            String refreshTokenKey = "refresh_token:" + userId;
            redisTemplate.delete(accessTokenKey);
            redisTemplate.delete(refreshTokenKey);
            logger.debug("从Redis中删除用户{}的令牌", userId);
        }
    }
    
    /**
     * 验证accessToken是否有效且存在于Redis中
     * @param token JWT令牌
     * @return token是否有效
     */
    public static String validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // 检查Redis中是否存在该accessToken
            if (redisTemplate != null) {
                String userId = claims.getSubject();
                String key = "access_token:" + userId;
                String storedToken = redisTemplate.opsForValue().get(key);
                
                // 检查Redis中存储的token是否与提供的token匹配
                if (storedToken != null && storedToken.equals(token)) {
                    logger.debug("accessToken验证成功，用户ID: {}", userId);
                    return userId;
                }
            }
            
            // 如果没有使用Redis或者Redis中没有找到匹配的token，则只验证JWT本身的有效性
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT access token已过期: " + e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT access token validation failed: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 验证refreshToken是否有效且存在于Redis中
     * @param token JWT令牌
     * @return token是否有效
     */
    public static String validateRefreshToken(String token) {
        try {
            logger.debug("开始验证refreshToken: {}", token);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            logger.debug("refreshToken解析成功，claims: {}", claims);
            
            // 检查Redis中是否存在该refreshToken
            if (redisTemplate != null) {
                String userId = claims.getSubject();
                String key = "refresh_token:" + userId;
                logger.debug("准备从Redis中获取key: {}", key);
                String storedToken = redisTemplate.opsForValue().get(key);
                logger.debug("从Redis中获取到的存储令牌: {}", storedToken);
                
                // 检查Redis中存储的token是否与提供的token匹配
                if (storedToken != null && storedToken.equals(token)) {
                    logger.debug("refreshToken验证成功，用户ID: {}", userId);
                    return userId;
                } else {
                    logger.warn("Redis中的令牌与提供的令牌不匹配。存储的令牌: {}, 提供的令牌: {}", storedToken, token);
                    // 检查是否是令牌过期问题
                    if (storedToken == null) {
                        logger.warn("Redis中未找到令牌，可能已过期或被删除");
                    }
                }
            } else {
                logger.warn("RedisTemplate为空，无法验证存储的令牌");
            }
            
            // 如果没有使用Redis或者Redis中没有找到匹配的token，则只验证JWT本身的有效性
            logger.debug("仅验证JWT本身的有效性，返回用户ID: {}", claims.getSubject());
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT refresh token已过期: " + e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT refresh token validation failed: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * 验证token是否有效且存在于Redis中（通用方法）
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