package com.lihuahua.hyperspace.models.dto;

import lombok.Data;
import java.util.Date;

@Data
public class MessageDTO {
    private String messageId;
    private String type;
    private String fromUserId;
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
    private Date createdAt;
    private String senderName;
    private Boolean showDate = false;
}