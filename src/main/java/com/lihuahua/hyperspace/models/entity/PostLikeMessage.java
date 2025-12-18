package com.lihuahua.hyperspace.models.entity;

import lombok.Data;

import java.util.Date;

/**
 * 帖子点赞消息实体类
 * 用于异步处理点赞操作
 */
@Data
public class PostLikeMessage {
    /**
     * 操作类型 (like-点赞, unlike-取消点赞)
     */
    private String action;
    
    /**
     * 帖子ID
     */
    private String postId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 创建时间
     */
    private Date createTime;
}