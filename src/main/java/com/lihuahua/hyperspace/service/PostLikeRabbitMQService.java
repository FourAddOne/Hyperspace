package com.lihuahua.hyperspace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.models.entity.PostLikeMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于RabbitMQ的帖子点赞服务
 */
@Service
public class PostLikeRabbitMQService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String LIKE_EXCHANGE = "post.like.exchange";
    private static final String LIKE_ROUTING_KEY = "post.like.routing.key";
    private static final String USER_RATE_LIMIT_KEY_PREFIX = "user_like_rate_limit:";
    
    /**
     * 发送点赞消息到RabbitMQ
     * @param message 点赞消息
     */
    public void sendLikeMessage(PostLikeMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(LIKE_EXCHANGE, LIKE_ROUTING_KEY, messageJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
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
        
        // 设置3秒内不能重复点赞（比之前更短的时间）
        redisTemplate.opsForValue().set(key, "1", 3, TimeUnit.SECONDS);
        return true;
    }
}