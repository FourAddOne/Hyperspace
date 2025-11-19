package com.lihuahua.hyperspace.rabbitmq.producer.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 聊天消息传输对象
 * 用于在RabbitMQ中传输聊天消息
 */
@Data
public class ChatMessageDTO implements Serializable {
    
    /**
     * 消息唯一标识
     */
    private String messageId;
    
    /**
     * 消息类型
     */
    private String type;
    
    /**
     * 发送者用户ID
     */
    private String fromUserId;
    
    /**
     * 发送者用户名
     */
    private String fromUsername;
    
    /**
     * 接收者ID（用户ID或群组ID）
     */
    private String toTargetId;
    
    /**
     * 接收者类型（USER或GROUP）
     */
    private String toTargetType;
    
    /**
     * 接收者名称
     */
    private String toTargetName;
    
    /**
     * 纯文本内容
     */
    private String textContent;
    
    /**
     * 图片URL列表
     */
    private List<String> imageUrls;
    
    /**
     * 富文本内容（包含图片位置信息的HTML格式文本）
     */
    private String richContent;
    
    /**
     * 客户端时间戳
     */
    private Long clientTimestamp;
    
    /**
     * 服务器时间戳
     */
    private Long serverTimestamp;
    
    /**
     * 消息状态
     */
    private String status;
    
    /**
     * IP地址
     */
    private String ip;
}