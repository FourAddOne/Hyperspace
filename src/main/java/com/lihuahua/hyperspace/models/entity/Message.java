package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("messages")
public class Message {
    @TableField("sender_id")
    private String senderId;

    @TableField("receiver_id")
    private String receiverId;

    @TableField("content")
    private String content;


    @TableId(value = "message_id")
    private String messageId;
    
    @TableField("conversation_id")
    private String conversationId;
    

    
    @TableField("content_type")
    private String contentType; // text, image, file, emoji, voice, video

    
    @TableField("file_url")
    private String fileUrl;
    
    @TableField("status")
    private String status; // sent, delivered, read
    
    @TableField("timestamp")
    private Date timestamp;


    
    // 添加索引注解以提高查询性能
    @TableField(exist = false)
    private String senderName; // 用于传输时的发送者名称，不在数据库中存储
}