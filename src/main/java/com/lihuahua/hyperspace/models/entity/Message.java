package com.lihuahua.hyperspace.models.entity;



import com.lihuahua.hyperspace.models.enums.MessageType;
import com.lihuahua.hyperspace.models.pojo.MessageContent;
import com.lihuahua.hyperspace.models.pojo.MessageExt;
import com.lihuahua.hyperspace.models.pojo.TargetInfo;
import com.lihuahua.hyperspace.models.pojo.UserInfo;
import lombok.Data;
import com.lihuahua.hyperspace.models.enums.MessageStatus;
import java.io.Serializable;
import java.util.Date;

/**
 * 消息实体类
 * 用于表示系统中的消息对象
 */
@Data
public class Message implements Serializable {
    // 消息唯一标识（全局唯一，如雪花ID或自定义拼接ID）
    private String messageId;

    // 消息类型：TEXT（纯文字）、IMAGE（纯图片）、MIXED（混合消息）、SYSTEM（系统消息）等
    private MessageType type;

    // 发送者信息
    private UserInfo from;

    // 接收者信息（单聊为用户，群聊为群组）
    private TargetInfo to;

    // 消息内容（根据type不同，结构不同）
    private MessageContent content;

    // 客户端发送时间戳（毫秒）
    private Long clientTimestamp;

    // 服务器接收时间戳（毫秒，用于全局排序）
    private Long serverTimestamp;

    // 消息状态：SENDING（发送中）、DELIVERED（已送达）、READ（已读）、FAILED（发送失败）
    private MessageStatus status;

    // 扩展字段（如引用消息ID、发送设备等）
    private MessageExt ext;

    // 数据库自增ID（仅存储用）
    private Long id;

    // 消息入库时间（仅存储用）
    private Date createTime;
    
    /**
     * 创建文本消息
     * @param from 发送者信息
     * @param to 接收者信息
     * @param text 文本内容
     * @return Message对象
     */
    public static Message createTextMessage(UserInfo from, TargetInfo to, String text) {
        Message message = new Message();
        message.messageId = generateMessageId();
        message.type = MessageType.TEXT;
        message.from = from;
        message.to = to;
        message.content = MessageContent.textContent(text);
        message.clientTimestamp = System.currentTimeMillis();
        message.serverTimestamp = System.currentTimeMillis();
        message.status = MessageStatus.SENDING;
        return message;
    }
    
    /**
     * 创建图片消息
     * @param from 发送者信息
     * @param to 接收者信息
     * @param imageUrls 图片URL列表
     * @return Message对象
     */
    public static Message createImageMessage(UserInfo from, TargetInfo to, java.util.List<String> imageUrls) {
        Message message = new Message();
        message.messageId = generateMessageId();
        message.type = MessageType.IMAGE;
        message.from = from;
        message.to = to;
        message.content = MessageContent.imageContent(imageUrls);
        message.clientTimestamp = System.currentTimeMillis();
        message.serverTimestamp = System.currentTimeMillis();
        message.status = MessageStatus.SENDING;
        return message;
    }
    
    /**
     * 创建混合消息
     * @param from 发送者信息
     * @param to 接收者信息
     * @param text 文本内容
     * @param imageUrls 图片URL列表
     * @return Message对象
     */
    public static Message createMixedMessage(UserInfo from, TargetInfo to, String text, java.util.List<String> imageUrls) {
        Message message = new Message();
        message.messageId = generateMessageId();
        message.type = MessageType.MIXED;
        message.from = from;
        message.to = to;
        message.content = MessageContent.mixedContent(text, imageUrls);
        message.clientTimestamp = System.currentTimeMillis();
        message.serverTimestamp = System.currentTimeMillis();
        message.status = MessageStatus.SENDING;
        return message;
    }
    
    /**
     * 创建富文本消息
     * @param from 发送者信息
     * @param to 接收者信息
     * @param richText 包含图片位置信息的HTML格式文本
     * @return Message对象
     */
    public static Message createRichMessage(UserInfo from, TargetInfo to, String richText) {
        Message message = new Message();
        message.messageId = generateMessageId();
        message.type = MessageType.MIXED; // 富文本消息也属于混合消息类型
        message.from = from;
        message.to = to;
        message.content = MessageContent.richContent(richText);
        message.clientTimestamp = System.currentTimeMillis();
        message.serverTimestamp = System.currentTimeMillis();
        message.status = MessageStatus.SENDING;
        return message;
    }
    
    /**
     * 生成消息ID（简化实现，实际项目中可能需要更复杂的ID生成策略）
     * @return 消息ID
     */
    private static String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}