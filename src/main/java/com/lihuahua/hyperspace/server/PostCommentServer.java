package com.lihuahua.hyperspace.server;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lihuahua.hyperspace.models.entity.PostComment;

import java.util.List;
import java.util.Set;

public interface PostCommentServer extends IService<PostComment> {
    /**
     * 根据帖子ID获取评论列表
     * @param postId 帖子ID
     * @return 评论列表
     */
    List<PostComment> getCommentsByPostId(String postId);
    
    /**
     * 切换评论点赞状态
     * @param commentId 评论ID
     * @param userId 用户ID
     * @param username 用户名
     * @return 操作结果
     */
    boolean toggleLike(String commentId, String userId, String username);
    
    /**
     * 使用RabbitMQ切换评论点赞状态
     * @param commentId 评论ID
     * @param userId 用户ID
     * @param username 用户名
     * @return 操作结果
     */
    boolean toggleLikeWithRabbitMQ(String commentId, String userId, String username);
    
    /**
     * 获取评论的点赞数
     * @param commentId 评论ID
     * @return 点赞数
     */
    int getCommentLikeCount(String commentId);
    
    /**
     * 检查用户是否已点赞某条评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    boolean isUserLikedComment(String commentId, String userId);
    
    /**
     * 预热用户对评论的点赞数据到Redis缓存
     * @param commentId 评论ID
     * @param userId 用户ID
     */
    void warmupUserCommentLikeData(String commentId, String userId);

    String warmupUserPostLikeData(String postId, String userId);

    /**
     * 点赞评论（使用RabbitMQ异步处理）
     *
     * @param commentId 评论ID
     * @param userId 用户ID
     * @param username 用户名
     * @return 点赞结果
     */
    boolean likeComment(String commentId, String userId, String username);
}