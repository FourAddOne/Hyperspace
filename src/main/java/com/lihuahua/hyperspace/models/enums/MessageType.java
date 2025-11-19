package com.lihuahua.hyperspace.models.enums;

/**
 * 消息类型枚举
 */
public enum MessageType {
    /**
     * 纯文字消息
     */
    TEXT,
    
    /**
     * 纯图片消息
     */
    IMAGE,
    
    /**
     * 混合消息（包含文字和图片等）
     */
    MIXED,
    
    /**
     * 系统消息
     */
    SYSTEM,
    
    /**
     * 音频消息
     */
    AUDIO,
    
    /**
     * 视频消息
     */
    VIDEO,
    
    /**
     * 文件消息
     */
    FILE
}