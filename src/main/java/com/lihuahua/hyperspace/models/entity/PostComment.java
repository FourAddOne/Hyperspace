package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 帖子评论实体类
 */
@Data
@TableName("`post_comment`")
public class PostComment {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 评论ID
     */
    @TableField("comment_id")
    private String commentId;

    /**
     * 帖子ID
     */
    @TableField("post_id")
    private String postId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 用户头像
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 评论内容
     */
    @TableField("content")
    private String content;

    /**
     * 父评论ID (用于回复评论)
     */
    @TableField("parent_id")
    private String parentId;

    /**
     * 被回复的用户ID
     */
    @TableField("reply_to_user_id")
    private String replyToUserId;

    /**
     * 被回复的用户名
     */
    @TableField("reply_to_username")
    private String replyToUsername;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount = 0;

    /**
     * 状态 (normal-正常, deleted-已删除)
     */
    @TableField("status")
    private String status = "normal";

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 媒体资源列表（非数据库字段，用于传输）
     */
    @TableField(exist = false)
    private List<PostMedia> mediaList;
    
    /**
     * 当前用户是否已点赞（非数据库字段，用于传输）
     */
    @TableField(exist = false)
    private boolean isLiked = false;
    
    // getter和setter方法
    public boolean getIsLiked() {
        return isLiked;
    }
    
    public void setIsLiked(boolean isLiked) {
        this.isLiked = isLiked;
    }
}