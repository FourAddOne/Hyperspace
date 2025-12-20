package com.lihuahua.hyperspace.models.dto;

import lombok.Data;
import java.util.Date;

@Data
public class MessageDTO {
    private String messageId;
    private String type;
    private String fromUserId;
    private String fromUsername; // 添加发送者用户名字段
    private String toTargetId;
    private String toTargetType;
    private String textContent;
    private String imageUrls;
    
    // 修改文件相关字段名称以匹配前端发送的字段
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    
    private Long clientTimestamp;
    private Long serverTimestamp;
    private String status;
    private String quoteMessageId;
    private String quoteMessageSenderName;
    private Date createdAt;
    private String senderName;
    private Boolean showDate = false;
    
    // 引用消息的相关字段
    private String quoteMessageContent;
    private String quoteMessageSenderId;
    
    // 被引用消息的详细信息
    private String quoteMessageType; // 被引用消息的类型(image/file/text)
    private String quoteMessageImageUrl; // 被引用图片消息的URL
    private String quoteMessageFileUrl; // 被引用文件消息的URL
    private String quoteMessageFileName; // 被引用文件消息的文件名
    private Long quoteMessageFileSize; // 被引用文件消息的文件大小
    
    // 添加用户头像URL字段
    private String userAvatarUrl;
}