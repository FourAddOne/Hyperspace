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

    // 添加文件相关字段
    @TableField("file_urls")
    private String fileUrls;

    @TableField("file_name")
    private String fileName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("client_timestamp")
    private Long clientTimestamp;

    @TableField("server_timestamp")
    private Long serverTimestamp;

    @TableField("status")
    private String status;

    @TableField("quote_message_id")
    private String quoteMessageId;
    
    @TableField("quote_message_sender_name")
    private String quoteMessageSenderName;

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
    
    // 添加根据quoteMessageId查询引用消息的方法
    public Message selectQuotedMessage() {
        // 这只是一个示例方法，实际实现应该在Mapper中完成
        return null;
    }
}