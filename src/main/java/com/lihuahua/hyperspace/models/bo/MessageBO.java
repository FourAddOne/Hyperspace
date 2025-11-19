package com.lihuahua.hyperspace.models.bo;

import com.lihuahua.hyperspace.models.enums.MessageStatus;
import com.lihuahua.hyperspace.models.enums.MessageType;
import com.lihuahua.hyperspace.models.pojo.MessageContent;
import com.lihuahua.hyperspace.models.pojo.MessageExt;
import com.lihuahua.hyperspace.models.pojo.TargetInfo;
import com.lihuahua.hyperspace.models.pojo.UserInfo;
import lombok.Data;

import java.util.Date;

/**
 * 消息业务对象类
 * 用于业务逻辑层处理
 */
@Data
public class MessageBO {
    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 消息类型
     */
    private MessageType type;

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
    private MessageStatus status;

    /**
     * 扩展字段
     */
    private MessageExt ext;

    /**
     * 数据库自增ID
     */
    private Long id;

    /**
     * 消息入库时间
     */
    private Date createTime;
}