package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 帖子媒体资源实体类
 */
@Data
@TableName("`post_media`")
public class PostMedia {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 媒体ID
     */
    @TableField("media_id")
    private String mediaId;

    /**
     * 帖子ID
     */
    @TableField("post_id")
    private String postId;

    /**
     * 媒体类型 (image-图片, video-视频)
     */
    @TableField("type")
    private String type;

    /**
     * 媒体URL
     */
    @TableField("url")
    private String url;

    /**
     * 缩略图URL
     */
    @TableField("thumbnail_url")
    private String thumbnailUrl;

    /**
     * 宽度
     */
    @TableField("width")
    private Integer width;

    /**
     * 高度
     */
    @TableField("height")
    private Integer height;

    /**
     * 文件大小(字节)
     */
    @TableField("size")
    private Long size;

    /**
     * 时长(秒)
     */
    @TableField("duration")
    private Integer duration;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder = 0;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
}