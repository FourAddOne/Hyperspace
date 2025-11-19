package com.lihuahua.hyperspace.models.dto;

import lombok.Data;

import java.util.Date;

@Data
public class MessageDTO {
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private Date createdAt;
    private String senderName;
    private boolean showDate; // 是否显示日期
}