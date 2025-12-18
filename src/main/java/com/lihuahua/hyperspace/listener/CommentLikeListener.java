package com.lihuahua.hyperspace.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.models.entity.CommentLikeMessage;
import com.lihuahua.hyperspace.server.PostCommentServer;
import com.lihuahua.hyperspace.models.entity.CommentLike;
import com.lihuahua.hyperspace.server.CommentLikeServer;
import com.lihuahua.hyperspace.utils.SnowflakeIdUtil;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 评论点赞消息监听器
 */
@Component
public class CommentLikeListener {
    
    @Autowired
    private PostCommentServer postCommentServer;
    
    @Autowired
    private CommentLikeServer commentLikeServer;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 监听评论点赞消息队列
     * @param messageJson 消息内容
     */
    @RabbitListener(queues = "comment.like.queue")
    public void handleCommentLikeMessage(String messageJson) {
        try {
            CommentLikeMessage message = objectMapper.readValue(messageJson, CommentLikeMessage.class);
            
            // 处理评论点赞消息
            if ("like".equals(message.getAction())) {
                // 点赞操作
                postCommentServer.toggleLike(
                    message.getCommentId(), 
                    message.getUserId(), 
                    message.getUsername()
                );
                
                // 保存点赞记录到数据库
                CommentLike commentLike = new CommentLike();
                commentLike.setLikeId("like_" + SnowflakeIdUtil.nextId());
                commentLike.setCommentId(message.getCommentId());
                commentLike.setUserId(message.getUserId());
                commentLike.setUsername(message.getUsername());
                commentLike.setCreateTime(new Date());
                commentLikeServer.save(commentLike);
            } else if ("unlike".equals(message.getAction())) {
                // 取消点赞操作
                postCommentServer.toggleLike(
                    message.getCommentId(), 
                    message.getUserId(), 
                    message.getUsername()
                );
                
                // 从数据库中删除点赞记录
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommentLike> queryWrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("comment_id", message.getCommentId())
                           .eq("user_id", message.getUserId());
                commentLikeServer.remove(queryWrapper);
            }
        } catch (Exception e) {
            // 记录详细的错误日志
            System.err.println("处理评论点赞消息失败: " + e.getMessage());
            System.err.println("失败的消息内容: " + messageJson);
            e.printStackTrace();
        }
    }
}