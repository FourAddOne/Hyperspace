package com.lihuahua.hyperspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.models.entity.PostLikeMessage;
import com.lihuahua.hyperspace.server.PostServer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 帖子点赞消息监听器
 * 监听RabbitMQ中的点赞消息并处理
 */
@Service
public class PostLikeMessageListener {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private PostServer postServer;
    
    /**
     * 监听并处理点赞消息
     * @param messageJson 点赞消息JSON字符串
     */
    @RabbitListener(queues = "post.like.queue")
    public void handleLikeMessage(String messageJson) {
        try {
            // 将JSON字符串转换为PostLikeMessage对象
            PostLikeMessage message = objectMapper.readValue(messageJson, PostLikeMessage.class);
            
            // 处理点赞操作
            postServer.toggleLike(
                message.getPostId(), 
                message.getUserId(), 
                message.getUsername()
            );
        } catch (Exception e) {
            // 记录错误日志，实际项目中应该使用日志框架
            System.err.println("处理点赞消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}