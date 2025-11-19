package com.lihuahua.hyperspace.utils;

import com.alibaba.fastjson.JSON;
import com.lihuahua.hyperspace.models.bo.MessageBO;
import com.lihuahua.hyperspace.models.dto.MessageDTO;
import com.lihuahua.hyperspace.models.dto.SendMessageDTO;
import com.lihuahua.hyperspace.models.entity.Message;
import com.lihuahua.hyperspace.models.enums.MessageStatus;
import com.lihuahua.hyperspace.models.enums.MessageType;
import com.lihuahua.hyperspace.models.enums.TargetType;
import com.lihuahua.hyperspace.models.pojo.MessageContent;
import com.lihuahua.hyperspace.models.pojo.MessageExt;
import com.lihuahua.hyperspace.models.pojo.TargetInfo;
import com.lihuahua.hyperspace.models.pojo.UserInfo;
import com.lihuahua.hyperspace.models.po.MessagePO;
import com.lihuahua.hyperspace.models.vo.MessageVO;
import com.lihuahua.hyperspace.rabbitmq.producer.dto.ChatMessageDTO;

import java.util.Date;

/**
 * 消息转换工具类
 * 用于在各种消息相关对象之间进行转换
 */
public class MessageConvertUtil {
    
    /**
     * 将 Message 实体转换为 ChatMessageDTO
     * @param message Message实体
     * @return ChatMessageDTO对象
     */
    public static ChatMessageDTO toChatMessageDTO(Message message) {
        if (message == null) {
            return null;
        }
        
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setMessageId(message.getMessageId());
        dto.setType(message.getType().name());
        
        // 检查发送者信息
        if (message.getFrom() != null) {
            dto.setFromUserId(message.getFrom().getUserId());
            dto.setFromUsername(message.getFrom().getUsername());
        }
        
        dto.setToTargetId(message.getTo().getTargetId());
        dto.setToTargetType(message.getTo().getTargetType().name());
        dto.setToTargetName(message.getTo().getTargetName());
        
        // 处理消息内容
        if (message.getContent() != null) {
            dto.setTextContent(message.getContent().getText());
            dto.setImageUrls(message.getContent().getImageUrls());
            dto.setRichContent(message.getContent().getRichText());
        }
        
        dto.setClientTimestamp(message.getClientTimestamp());
        dto.setServerTimestamp(message.getServerTimestamp());
        dto.setStatus(message.getStatus().name());
        
        // 处理扩展信息
        if (message.getExt() != null) {
            dto.setIp(message.getExt().getDeviceType()); // 简单处理，实际应该有专门的IP字段
        }
        
        return dto;
    }
    
    /**
     * 将 Message 实体转换为 MessagePO
     * @param message Message实体
     * @return MessagePO对象
     */
    public static MessagePO toPO(Message message) {
        if (message == null) {
            return null;
        }
        
        MessagePO po = new MessagePO();
        po.setMessageId(message.getMessageId());
        po.setType(message.getType().name());
        
        // 检查发送者信息
        if (message.getFrom() != null) {
            po.setFromUserId(message.getFrom().getUserId());
            po.setFromUsername(message.getFrom().getUsername());
        }
        
        // 检查接收者信息
        if (message.getTo() != null) {
            po.setToTargetId(message.getTo().getTargetId());
            po.setToTargetType(message.getTo().getTargetType().name());
            po.setToTargetName(message.getTo().getTargetName());
        }
        
        // 处理消息内容
        if (message.getContent() != null) {
            po.setTextContent(message.getContent().getText());
            po.setRichContent(message.getContent().getRichText());
            if (message.getContent().getImageUrls() != null) {
                po.setImageUrls(JSON.toJSONString(message.getContent().getImageUrls()));
            }
        }
        
        po.setClientTimestamp(message.getClientTimestamp());
        po.setServerTimestamp(message.getServerTimestamp());
        po.setStatus(message.getStatus().name());
        
        // 处理扩展信息
        if (message.getExt() != null) {
            po.setQuoteMessageId(message.getExt().getQuoteMessageId());
            po.setDeviceType(message.getExt().getDeviceType());
            po.setDeviceName(message.getExt().getDeviceName());
        }
        
        po.setCreateTime(new Date());
        po.setUpdateTime(new Date());
        
        return po;
    }
    
    /**
     * 将 MessagePO 转换为 Message 实体
     * @param po MessagePO对象
     * @return Message实体
     */
    public static Message toEntity(MessagePO po) {
        if (po == null) {
            return null;
        }
        
        Message message = new Message();
        message.setMessageId(po.getMessageId());
        
        // 转换消息类型
        try {
            message.setType(MessageType.valueOf(po.getType()));
        } catch (Exception e) {
            message.setType(MessageType.TEXT); // 默认值
        }
        
        // 转换发送者信息（如果存在）
        if (po.getFromUserId() != null) {
            UserInfo from = new UserInfo();
            from.setUserId(po.getFromUserId());
            from.setUsername(po.getFromUsername());
            message.setFrom(from);
        }
        
        // 转换接收者信息（如果存在）
        if (po.getToTargetId() != null) {
            TargetInfo to = new TargetInfo();
            to.setTargetId(po.getToTargetId());
            try {
                to.setTargetType(TargetType.valueOf(po.getToTargetType()));
            } catch (Exception e) {
                to.setTargetType(TargetType.USER); // 默认值
            }
            to.setTargetName(po.getToTargetName());
            message.setTo(to);
        }
        
        // 转换消息内容
        MessageContent content = new MessageContent();
        content.setText(po.getTextContent());
        content.setRichText(po.getRichContent());
        if (po.getImageUrls() != null) {
            try {
                content.setImageUrls(JSON.parseArray(po.getImageUrls(), String.class));
            } catch (Exception e) {
                // 解析失败则忽略
            }
        }
        message.setContent(content);
        
        message.setClientTimestamp(po.getClientTimestamp());
        message.setServerTimestamp(po.getServerTimestamp());
        
        // 转换消息状态
        try {
            message.setStatus(MessageStatus.valueOf(po.getStatus()));
        } catch (Exception e) {
            message.setStatus(MessageStatus.SENDING); // 默认值
        }
        
        // 转换扩展信息
        MessageExt ext = new MessageExt();
        ext.setQuoteMessageId(po.getQuoteMessageId());
        ext.setDeviceType(po.getDeviceType());
        ext.setDeviceName(po.getDeviceName());
        message.setExt(ext);
        
        message.setId(po.getId());
        message.setCreateTime(po.getCreateTime());
        
        return message;
    }
    
    /**
     * 将 Message 实体转换为 MessageVO
     * @param message Message实体
     * @return MessageVO对象
     */
    public static MessageVO toVO(Message message) {
        if (message == null) {
            return null;
        }
        
        return MessageVO.builder()
                .messageId(message.getMessageId())
                .type(message.getType().name())
                .from(message.getFrom())
                .to(message.getTo())
                .content(message.getContent())
                .clientTimestamp(message.getClientTimestamp())
                .serverTimestamp(message.getServerTimestamp())
                .status(message.getStatus().name())
                .ext(message.getExt())
                .build();
    }
    
    /**
     * 将 MessageDTO 转换为 Message 实体
     * @param dto MessageDTO对象
     * @return Message实体
     */
    public static Message toEntity(MessageDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Message message = new Message();
        message.setMessageId(dto.getMessageId());
        
        // 转换消息类型
        try {
            message.setType(MessageType.valueOf(dto.getType()));
        } catch (Exception e) {
            message.setType(MessageType.TEXT);
        }
        
        // 转换发送者信息
        UserInfo from = new UserInfo();
        from.setUserId(dto.getFromUserId());
        from.setUsername(dto.getFromUsername());
        message.setFrom(from);
        
        // 转换接收者信息
        TargetInfo to = new TargetInfo();
        to.setTargetId(dto.getToTargetId());
        try {
            to.setTargetType(TargetType.valueOf(dto.getToTargetType()));
        } catch (Exception e) {
            to.setTargetType(TargetType.USER);
        }
        to.setTargetName(dto.getToTargetName());
        message.setTo(to);
        
        // 转换消息内容
        MessageContent content = new MessageContent();
        content.setText(dto.getTextContent());
        content.setImageUrls(dto.getImageUrls());
        content.setRichText(dto.getRichContent());
        message.setContent(content);
        
        message.setClientTimestamp(dto.getClientTimestamp());
        message.setServerTimestamp(System.currentTimeMillis());
        
        // 转换消息状态
        try {
            message.setStatus(MessageStatus.valueOf(dto.getStatus()));
        } catch (Exception e) {
            message.setStatus(MessageStatus.SENDING);
        }
        
        // 转换扩展信息
        MessageExt ext = new MessageExt();
        ext.setQuoteMessageId(dto.getQuoteMessageId());
        ext.setDeviceType(dto.getDeviceType());
        ext.setDeviceName(dto.getDeviceName());
        message.setExt(ext);
        
        return message;
    }
    
    /**
     * 将 SendMessageDTO 转换为 Message 实体
     * @param dto SendMessageDTO对象
     * @param fromUserId 发送者用户ID
     * @param fromUsername 发送者用户名
     * @return Message实体
     */
    public static Message toEntity(SendMessageDTO dto, String fromUserId, String fromUsername) {
        if (dto == null) {
            return null;
        }
        
        Message message = new Message();
        message.setMessageId(generateMessageId());
        
        // 转换消息类型
        try {
            message.setType(MessageType.valueOf(dto.getType()));
        } catch (Exception e) {
            message.setType(MessageType.TEXT);
        }
        
        // 设置发送者信息
        if (fromUserId != null && !fromUserId.isEmpty()) {
            UserInfo from = new UserInfo();
            from.setUserId(fromUserId);
            message.setFrom(from);
        }
        
        // 转换接收者信息
        TargetInfo to = new TargetInfo();
        to.setTargetId(dto.getToTargetId());
        try {
            to.setTargetType(TargetType.valueOf(dto.getToTargetType()));
        } catch (Exception e) {
            to.setTargetType(TargetType.USER);
        }
        message.setTo(to);
        
        // 转换消息内容
        MessageContent content = new MessageContent();
        content.setText(dto.getTextContent());
        content.setImageUrls(dto.getImageUrls());
        content.setRichText(dto.getRichContent());
        message.setContent(content);
        
        message.setClientTimestamp(System.currentTimeMillis());
        message.setServerTimestamp(System.currentTimeMillis());
        message.setStatus(MessageStatus.SENDING);
        
        // 转换扩展信息
        MessageExt ext = new MessageExt();
        ext.setQuoteMessageId(dto.getQuoteMessageId());
        ext.setDeviceType(dto.getDeviceType());
        ext.setDeviceName(dto.getDeviceName());
        message.setExt(ext);
        
        return message;
    }
    
    /**
     * 将 Message 实体转换为 MessageBO
     * @param message Message实体
     * @return MessageBO对象
     */
    public static MessageBO toBO(Message message) {
        if (message == null) {
            return null;
        }
        
        MessageBO bo = new MessageBO();
        bo.setMessageId(message.getMessageId());
        bo.setType(message.getType());
        bo.setFrom(message.getFrom());
        bo.setTo(message.getTo());
        bo.setContent(message.getContent());
        bo.setClientTimestamp(message.getClientTimestamp());
        bo.setServerTimestamp(message.getServerTimestamp());
        bo.setStatus(message.getStatus());
        bo.setExt(message.getExt());
        bo.setId(message.getId());
        bo.setCreateTime(message.getCreateTime());
        
        return bo;
    }
    
    /**
     * 生成消息ID
     * @return 消息ID
     */
    private static String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 将 SendMessageDTO 转换为 Message 实体
     * @param sendMessageDTO SendMessageDTO对象
     * @return Message实体
     */
    public static Message toEntity(SendMessageDTO sendMessageDTO) {
        if (sendMessageDTO == null) {
            return null;
        }
        
        Message message = new Message();
        message.setMessageId(generateMessageId());
        
        // 转换消息类型
        try {
            message.setType(MessageType.valueOf(sendMessageDTO.getType()));
        } catch (Exception e) {
            message.setType(MessageType.TEXT);
        }
        
        // 转换接收者信息
        TargetInfo to = new TargetInfo();
        to.setTargetId(sendMessageDTO.getToTargetId());
        try {
            to.setTargetType(TargetType.valueOf(sendMessageDTO.getToTargetType()));
        } catch (Exception e) {
            to.setTargetType(TargetType.USER);
        }
        message.setTo(to);
        
        // 转换消息内容
        MessageContent content = new MessageContent();
        content.setText(sendMessageDTO.getTextContent());
        content.setImageUrls(sendMessageDTO.getImageUrls());
        content.setRichText(sendMessageDTO.getRichContent());
        message.setContent(content);
        
        message.setClientTimestamp(System.currentTimeMillis());
        message.setServerTimestamp(System.currentTimeMillis());
        message.setStatus(MessageStatus.SENDING);
        
        // 转换扩展信息
        MessageExt ext = new MessageExt();
        ext.setQuoteMessageId(sendMessageDTO.getQuoteMessageId());
        ext.setDeviceType(sendMessageDTO.getDeviceType());
        ext.setDeviceName(sendMessageDTO.getDeviceName());
        message.setExt(ext);
        
        return message;
    }
    
    /**
     * 将 MessageBO 转换为 Message 实体
     * @param message MessageBO对象
     * @return Message实体
     */
    public static Message toEntity(MessageBO message) {
        if (message == null) {
            return null;
        }
        
        Message entity = new Message();
        entity.setMessageId(message.getMessageId());
        entity.setType(message.getType());
        entity.setFrom(message.getFrom());
        entity.setTo(message.getTo());
        entity.setContent(message.getContent());
        entity.setClientTimestamp(message.getClientTimestamp());
        entity.setServerTimestamp(message.getServerTimestamp());
        entity.setStatus(message.getStatus());
        entity.setExt(message.getExt());
        entity.setId(message.getId());
        entity.setCreateTime(message.getCreateTime());
        
        return entity;
    }
}