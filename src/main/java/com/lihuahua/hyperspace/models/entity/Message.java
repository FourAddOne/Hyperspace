package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("`message`")
public class Message {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("message_id")
    private String messageId;

    @TableField("type")
    private String type;

    @TableField("from_user_id")
    private String fromUserId;

    @TableField("from_username")
    private String fromUsername;

    @TableField("to_target_id")
    private String toTargetId;

    @TableField("to_target_type")
    private String toTargetType;

    @TableField("to_target_name")
    private String toTargetName;

    @TableField("text_content")
    private String textContent;

    @TableField("image_urls")
    private String imageUrls;

    @TableField("client_timestamp")
    private Long clientTimestamp;

    @TableField("server_timestamp")
    private Long serverTimestamp;

    @TableField("status")
    private String status;

    @TableField("quote_message_id")
    private String quoteMessageId;

    @TableField("device_type")
    private String deviceType;

    @TableField("device_name")
    private String deviceName;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField("rich_content")
    private String richContent;
}