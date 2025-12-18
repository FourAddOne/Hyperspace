package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 帖子实体类
 */
@Data
@TableName("`post`")
public class Post {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
     * 用户头像 (存储相对路径)
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 帖子类型 (video-视频, article-文章)
     */
    @TableField("type")
    private String type;

    /**
     * 标题
     */
    @TableField("title")
    private String title;

    /**
     * 内容
     */
    @TableField("content")
    private String content;

    /**
     * 封面图片URL (存储相对路径)
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 视频URL
     */
    @TableField("video_url")
    private String videoUrl;

    /**
     * 视频时长(秒)
     */
    @TableField("duration")
    private Integer duration;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Integer viewCount = 0;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount = 0;

    /**
     * 评论数
     */
    @TableField("comment_count")
    private Integer commentCount = 0;

    /**
     * 分享数
     */
    @TableField("share_count")
    private Integer shareCount = 0;

    /**
     * 状态 (published-已发布, draft-草稿, deleted-已删除)
     */
    @TableField("status")
    private String status = "published";

    /**
     * 标签 (JSON格式)
     */
    @TableField("tags")
    private String tags;

    /**
     * 分类
     */
    @TableField("category")
    private String category;

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