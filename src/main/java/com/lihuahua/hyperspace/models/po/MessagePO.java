package com.lihuahua.hyperspace.models.po;

import com.baomidou.mybatisplus.annotation.*;
import com.lihuahua.hyperspace.models.enums.MessageStatus;
import com.lihuahua.hyperspace.models.enums.MessageType;
import com.lihuahua.hyperspace.models.enums.TargetType;
import lombok.Data;

import java.util.Date;

/**
 * 消息持久化对象
 * 用于与数据库进行交互
 */
@Data
@TableName("\"message\"")
public class MessagePO {
    
    /**
     * 数据库自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 消息唯一标识
     */
    @TableField("message_id")
    private String messageId;
    
    /**
     * 消息类型
     */
    @TableField("type")
    private String type;
    
    /**
     * 发送者ID
     */
    @TableField("from_user_id")
    private String fromUserId;
    
    /**
     * 发送者名称
     */
    @TableField("from_username")
    private String fromUsername;
    
    /**
     * 接收者ID
     */
    @TableField("to_target_id")
    private String toTargetId;
    
    /**
     * 接收者类型
     */
    @TableField("to_target_type")
    private String toTargetType;
    
    /**
     * 接收者名称
     */
    @TableField("to_target_name")
    private String toTargetName;
    
    /**
     * 文本内容
     */
    @TableField("text_content")
    private String textContent;
    
    /**
     * 图片URL（JSON格式存储）
     */
    @TableField("image_urls")
    private String imageUrls;

    /**
     * 富文本内容（包含图片位置信息的HTML格式文本）
     */
    @TableField("rich_content")
    private String richContent;


    /**
     * 客户端时间戳
     */
    @TableField("client_timestamp")
    private Long clientTimestamp;
    
    /**
     * 服务器时间戳
     */
    @TableField("server_timestamp")
    private Long serverTimestamp;
    
    /**
     * 消息状态
     */
    @TableField("status")
    private String status;
    
    /**
     * 引用消息ID
     */
    @TableField("quote_message_id")
    private String quoteMessageId;
    
    /**
     * 设备类型
     */
    @TableField("device_type")
    private String deviceType;
    
    /**
     * 设备名称
     */
    @TableField("device_name")
    private String deviceName;
    
    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;
    
    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;
}