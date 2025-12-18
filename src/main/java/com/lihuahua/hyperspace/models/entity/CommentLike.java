package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 评论点赞实体类
 */
@Data
@TableName("`comment_like`")
public class CommentLike {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 点赞ID
     */
    @TableField("like_id")
    private String likeId;

    /**
     * 评论ID
     */
    @TableField("comment_id")
    private String commentId;

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
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
}