package com.lihuahua.hyperspace.models.enums;

/**
 * 消息状态枚举
 */
public enum MessageStatus {
    /**
     * 发送中
     */
    SENDING,
    
    /**
     * 已送达
     */
    DELIVERED,
    
    /**
     * 已读
     */
    READ,
    
    /**
     * 发送失败
     */
    FAILED
}