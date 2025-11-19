package com.lihuahua.hyperspace.models.vo;

import com.lihuahua.hyperspace.models.pojo.MessageContent;
import com.lihuahua.hyperspace.models.pojo.MessageExt;
import com.lihuahua.hyperspace.models.pojo.TargetInfo;
import com.lihuahua.hyperspace.models.pojo.UserInfo;
import lombok.Builder;
import lombok.Data;

/**
 * 消息视图对象
 * 用于向前端返回消息数据
 */
@Data
@Builder
public class MessageVO {
    
    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 发送者信息
     */
    private UserInfo from;

    /**
     * 接收者信息
     */
    private TargetInfo to;

    /**
     * 消息内容
     */
    private MessageContent content;

    /**
     * 客户端发送时间戳
     */
    private Long clientTimestamp;

    /**
     * 服务器接收时间戳
     */
    private Long serverTimestamp;

    /**
     * 消息状态
     */
    private String status;

    /**
     * 扩展字段
     */
    private MessageExt ext;
    
    /**
     * IP地址
     */
    private String ip;
}