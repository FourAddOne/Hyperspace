package com.lihuahua.hyperspace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.models.entity.PostLikeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 帖子点赞消息服务
 * 使用Redis作为消息队列处理点赞操作
 */
@Service
public class PostLikeMessageService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String LIKE_QUEUE_KEY = "post_like_queue";
    private static final String USER_RATE_LIMIT_KEY_PREFIX = "user_like_rate_limit:";
    
    /**
     * 异步发送点赞消息到队列
     * @param message 点赞消息
     */
    @Async
    public void sendLikeMessage(PostLikeMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(LIKE_QUEUE_KEY, messageJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 从队列中获取点赞消息
     * @return 点赞消息
     */
    public PostLikeMessage receiveLikeMessage() {
        try {
            String messageJson = redisTemplate.opsForList().rightPop(LIKE_QUEUE_KEY);
            if (messageJson != null) {
                return objectMapper.readValue(messageJson, PostLikeMessage.class);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 检查用户点赞频率限制
     * @param userId 用户ID
     * @return 是否允许点赞
     */
    public boolean isUserAllowedToLike(String userId) {
        String key = USER_RATE_LIMIT_KEY_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(key);
        
        if (value != null) {
            // 用户在限制时间内已经点过赞
            return false;
        }
        
        // 设置10秒内不能重复点赞
        redisTemplate.opsForValue().set(key, "1", 10, TimeUnit.SECONDS);
        return true;
    }
}