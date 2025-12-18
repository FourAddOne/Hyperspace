package com.lihuahua.hyperspace.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.models.entity.PostLikeMessage;
import com.lihuahua.hyperspace.server.PostServer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 帖子点赞消息监听器
 */
@Component
public class PostLikeListener {
    
    @Autowired
    private PostServer postServer;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 监听点赞消息队列
     * @param messageJson 消息内容
     */
    @RabbitListener(queues = "post.like.queue")
    public void handlePostLikeMessage(String messageJson) {
        try {
            PostLikeMessage message = objectMapper.readValue(messageJson, PostLikeMessage.class);
            
            // 根据消息中的action字段决定是点赞还是取消点赞
            if ("unlike".equals(message.getAction())) {
                // 取消点赞操作
                postServer.toggleLike(
                    message.getPostId(), 
                    message.getUserId(), 
                    message.getUsername()
                );
            } else if ("like".equals(message.getAction())) {
                // 点赞操作
                postServer.toggleLike(
                    message.getPostId(), 
                    message.getUserId(), 
                    message.getUsername()
                );
            } else {
                // 默认行为（向后兼容）
                postServer.toggleLike(
                    message.getPostId(), 
                    message.getUserId(), 
                    message.getUsername()
                );
            }
        } catch (Exception e) {
            // 记录详细的错误日志
            System.err.println("处理点赞消息失败: " + e.getMessage());
            System.err.println("失败的消息内容: " + messageJson);
            e.printStackTrace();
        }
    }
}