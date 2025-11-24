package com.lihuahua.hyperspace.models.dto;

import lombok.Data;

import java.util.Date;

@Data
public class MessageDTO {
    private String id;
    private String messageId;
    private String type;
    private String fromUserId;
    private String fromUsername;
    private String toTargetId;
    private String toTargetType;
    private String toTargetName;
    private String textContent;
    private String imageUrls;
    private Long clientTimestamp;
    private Long serverTimestamp;
    private String status;
    private String quoteMessageId;
    private String deviceType;
    private String deviceName;
    private Date createTime;
    private Date updateTime;
    private String richContent;
    private Date createdAt;
    private String senderName;
    private boolean showDate; // 是否显示日期
}