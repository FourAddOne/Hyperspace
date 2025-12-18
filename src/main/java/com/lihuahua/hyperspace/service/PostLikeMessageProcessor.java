package com.lihuahua.hyperspace.service;

import com.lihuahua.hyperspace.models.entity.PostLikeMessage;
import com.lihuahua.hyperspace.server.PostServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 帖子点赞消息处理器
 * 定时从队列中获取点赞消息并处理
 */
@Component
public class PostLikeMessageProcessor {
    
    @Autowired
    private PostLikeMessageService postLikeMessageService;
    
    @Autowired
    private PostServer postServer;
    
    /**
     * 定时处理点赞消息队列
     * 每秒执行一次
     */
    @Scheduled(fixedDelay = 1000)
    public void processLikeMessages() {
        PostLikeMessage message;
        while ((message = postLikeMessageService.receiveLikeMessage()) != null) {
            try {
                // 处理点赞消息
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
}