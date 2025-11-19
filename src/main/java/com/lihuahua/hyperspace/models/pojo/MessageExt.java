package com.lihuahua.hyperspace.models.pojo;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息扩展信息类
 * 用于存储消息的额外信息，如引用消息、设备信息等
 */
@Data
public class MessageExt implements Serializable {
    /**
     * 引用消息ID（用于回复消息）
     */
    private String quoteMessageId;
    
    /**
     * 发送设备类型
     */
    private String deviceType;
    
    /**
     * 发送设备名称
     */
    private String deviceName;
    
    /**
     * 自定义扩展字段
     */
    private String customData;
    
    /**
     * 创建引用消息扩展信息
     * @param quoteMessageId 引用消息ID
     * @return MessageExt对象
     */
    public static MessageExt quoteMessage(String quoteMessageId) {
        MessageExt ext = new MessageExt();
        ext.quoteMessageId = quoteMessageId;
        return ext;
    }
    
    /**
     * 创建设备信息扩展信息
     * @param deviceType 设备类型
     * @param deviceName 设备名称
     * @return MessageExt对象
     */
    public static MessageExt deviceInfo(String deviceType, String deviceName) {
        MessageExt ext = new MessageExt();
        ext.deviceType = deviceType;
        ext.deviceName = deviceName;
        return ext;
    }
}