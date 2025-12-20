package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.mapper.MessageMapper;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.models.dto.MessageDTO;
import com.lihuahua.hyperspace.models.entity.Message;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.server.MessageServer;
import com.lihuahua.hyperspace.utils.OssUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.BeansException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageServerImpl implements MessageServer, ApplicationContextAware {
    
    @Resource
    private MessageMapper messageMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private OssUtil ossUtil;
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean sendGroupMessage(MessageDTO messageDTO) {
        Message message = new Message();
        BeanUtils.copyProperties(messageDTO, message);
        // 生成业务主键
        message.setMessageId("msg_" + java.util.UUID.randomUUID().toString().replace("-", ""));
        message.setType(messageDTO.getType() != null ? messageDTO.getType() : "text");
        message.setStatus("success");
        message.setServerTimestamp(System.currentTimeMillis());
        message.setClientTimestamp(messageDTO.getClientTimestamp() != null ? messageDTO.getClientTimestamp() : System.currentTimeMillis());
        message.setCreateTime(new Date());
        message.setUpdateTime(new Date());
        // 设置目标类型为group
        message.setToTargetType("group");
        
        // 处理图片URL，将其转换为相对路径存储在数据库中
        if (messageDTO.getImageUrls() != null && !messageDTO.getImageUrls().isEmpty()) {
            // 直接转换为相对路径
            String relativePath = ossUtil.extractObjectNameFromUrl(messageDTO.getImageUrls());
            message.setImageUrls(relativePath);
        }
        
        // 设置发送者用户名
        User sender = userMapper.selectById(messageDTO.getFromUserId());
        if (sender != null) {
            message.setFromUsername(sender.getUserName());
        }
        
        messageMapper.insert(message);
        return true;
    }

    @Override
    public boolean sendMessage(MessageDTO messageDTO) {
        try {
            Message message = new Message();
            // 生成业务主键
            message.setMessageId("msg_" + java.util.UUID.randomUUID().toString().replace("-", ""));
            
            // 检查是否是引用消息，如果是则设置type为quote，否则使用传入的type或默认为text
            if (messageDTO.getQuoteMessageId() != null && !messageDTO.getQuoteMessageId().isEmpty()) {
                message.setType("quote");
            } else {
                message.setType(messageDTO.getType() != null ? messageDTO.getType() : "text");
            }
            
            message.setFromUserId(messageDTO.getFromUserId());
            message.setToTargetId(messageDTO.getToTargetId());
            message.setToTargetType("user");
            message.setTextContent(messageDTO.getTextContent());
            
            // 处理图片URL，将其转换为相对路径存储在数据库中
            if (messageDTO.getImageUrls() != null && !messageDTO.getImageUrls().isEmpty()) {
                // 直接转换为相对路径
                String relativePath = ossUtil.extractObjectNameFromUrl(messageDTO.getImageUrls());
                message.setImageUrls(relativePath);
            }
            
            // 处理文件URL，将其转换为相对路径存储在数据库中
            if (messageDTO.getFileUrl() != null && !messageDTO.getFileUrl().isEmpty()) {
                // 直接转换为相对路径
                String relativePath = ossUtil.extractObjectNameFromUrl(messageDTO.getFileUrl());
                message.setFileUrls(relativePath);
                // 文件名和文件大小是可选的
                if (messageDTO.getFileName() != null && !messageDTO.getFileName().isEmpty()) {
                    message.setFileName(messageDTO.getFileName());
                }
                if (messageDTO.getFileSize() != null) {
                    message.setFileSize(messageDTO.getFileSize());
                }
            }
            
            // 设置引用消息ID和被引用消息的发送者名称
            if (messageDTO.getQuoteMessageId() != null && !messageDTO.getQuoteMessageId().isEmpty()) {
                message.setQuoteMessageId(messageDTO.getQuoteMessageId());
                
                // 获取被引用的消息并设置发送者名称
                Message quotedMessage = messageMapper.getMessageById(messageDTO.getQuoteMessageId());
                if (quotedMessage != null) {
                    if (quotedMessage.getFromUsername() != null) {
                        message.setQuoteMessageSenderName(quotedMessage.getFromUsername());
                    } else {
                        // 如果消息中没有发送者名称，则从用户表中查询
                        User quotedMessageSender = userMapper.selectById(quotedMessage.getFromUserId());
                        if (quotedMessageSender != null) {
                            message.setQuoteMessageSenderName(quotedMessageSender.getUserName());
                        }
                    }
                }
            }
            
            // 设置发送者用户名（无论是否是引用消息）
            User sender = userMapper.selectById(messageDTO.getFromUserId());
            if (sender != null) {
                message.setFromUsername(sender.getUserName());
            }
            
            message.setClientTimestamp(messageDTO.getClientTimestamp() != null ? messageDTO.getClientTimestamp() : System.currentTimeMillis());
            message.setServerTimestamp(System.currentTimeMillis());
            message.setStatus("success");
            message.setCreateTime(new Date());
            message.setUpdateTime(new Date());
            
            // 使用自定义插入方法
            int result = messageMapper.insertMessage(message);

            
            // 更新messageDTO的ID
            messageDTO.setMessageId(message.getMessageId());
            messageDTO.setServerTimestamp(message.getServerTimestamp());
            messageDTO.setCreatedAt(new Date(message.getServerTimestamp()));
            
            // 检查是否是当天第一条消息
            messageDTO.setShowDate(isFirstMessageToday(message.getFromUserId(), message.getToTargetId()));
            
            // 如果是引用消息，也填充引用消息的相关信息
            if (message.getQuoteMessageId() != null && !message.getQuoteMessageId().isEmpty()) {
                // 获取被引用的消息
                Message quotedMessage = messageMapper.getMessageById(message.getQuoteMessageId());
                if (quotedMessage != null) {
                    // 设置被引用消息的发送者名称
                         if (quotedMessage.getFromUsername() != null) {
                        messageDTO.setQuoteMessageSenderName(quotedMessage.getFromUsername());
                    } else {
                        // 如果消息中没有发送者名称，则从用户表中查询
                        User quotedMessageSender = userMapper.selectById(quotedMessage.getFromUserId());
                        if (quotedMessageSender != null) {
                            messageDTO.setQuoteMessageSenderName(quotedMessageSender.getUserName());
                        }
                    }

                    // 根据被引用消息的类型设置不同的内容
                    if ("image".equals(quotedMessage.getType())) {
                        messageDTO.setQuoteMessageContent("[图片]");
                        messageDTO.setQuoteMessageType("image");
                        
                        // 设置图片URL
                        if (quotedMessage.getImageUrls() != null && !quotedMessage.getImageUrls().isEmpty()) {
                            String fullUrl = ossUtil.convertToFullUrl(quotedMessage.getImageUrls());
                            messageDTO.setQuoteMessageImageUrl(fullUrl);
                        }
                    } else if ("file".equals(quotedMessage.getType())) {
                        messageDTO.setQuoteMessageContent("[文件] " + (quotedMessage.getFileName() != null ? quotedMessage.getFileName() : ""));
                        messageDTO.setQuoteMessageType("file");
                        
                        // 设置文件相关信息
                        if (quotedMessage.getFileUrls() != null && !quotedMessage.getFileUrls().isEmpty()) {
                            String fullUrl = ossUtil.convertToFullUrl(quotedMessage.getFileUrls());
                            messageDTO.setQuoteMessageFileUrl(fullUrl);
                        }
                        messageDTO.setQuoteMessageFileName(quotedMessage.getFileName());
                        if (quotedMessage.getFileSize() != null) {
                            messageDTO.setQuoteMessageFileSize(quotedMessage.getFileSize());
                        }
                    } else {
                        // 默认文本消息
                        messageDTO.setQuoteMessageContent(quotedMessage.getTextContent());
                        messageDTO.setQuoteMessageType("text");
                          }
                }
            }
            
            return result > 0;
        } catch (Exception e) {
                e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public List<MessageDTO> getChatHistory(String userId, String friendId) {
        try {
            List<Message> messages = messageMapper.getChatHistory(userId, friendId);
            List<MessageDTO> messageDTOs = new ArrayList<>();
            
            // 获取发送者用户信息
            User sender = userMapper.selectById(friendId);
            
            // 用于跟踪每天是否已经显示过日期
            Set<LocalDate> datesWithShownDate = new HashSet<>();
            
            for (Message message : messages) {
                MessageDTO dto = new MessageDTO();
                dto.setMessageId(message.getMessageId());
                dto.setType(message.getType());
                dto.setFromUserId(message.getFromUserId());
                dto.setToTargetId(message.getToTargetId());
                dto.setTextContent(message.getTextContent());
                
                // 处理图片URL，将其转换为完整URL
                if (message.getImageUrls() != null && !message.getImageUrls().isEmpty()) {
                    String fullUrl = ossUtil.convertToFullUrl(message.getImageUrls());
                    dto.setImageUrls(fullUrl);
                }
                
                // 处理文件URL，将其转换为完整URL
                if (message.getFileUrls() != null && !message.getFileUrls().isEmpty()) {
                    String fullUrl = ossUtil.convertToFullUrl(message.getFileUrls());
                    dto.setFileUrl(fullUrl);
                    // 文件名和文件大小是可选的
                    if (message.getFileName() != null && !message.getFileName().isEmpty()) {
                        dto.setFileName(message.getFileName());
                    }
                    if (message.getFileSize() != null) {
                        dto.setFileSize(message.getFileSize());
                    }
                }
                
                // 处理引用消息
                if (message.getQuoteMessageId() != null && !message.getQuoteMessageId().isEmpty()) {
                    dto.setQuoteMessageId(message.getQuoteMessageId());
                    
                    // 直接从消息中获取被引用消息的发送者名称
                    if (message.getQuoteMessageSenderName() != null) {
                        dto.setQuoteMessageSenderName(message.getQuoteMessageSenderName());
                    }
                    
                    // 获取被引用消息的内容
                       Message quotedMessage = messageMapper.getMessageById(message.getQuoteMessageId());
                       if (quotedMessage != null) {
                        // 如果数据库中没有存储发送者名称，则设置它
                        if (dto.getQuoteMessageSenderName() == null) {
                            // 设置被引用消息的发送者名称
                               if (quotedMessage.getFromUsername() != null) {
                                dto.setQuoteMessageSenderName(quotedMessage.getFromUsername());
                            } else {
                                // 如果消息中没有发送者名称，则从用户表中查询
                                User quotedMessageSender = userMapper.selectById(quotedMessage.getFromUserId());
                                if (quotedMessageSender != null) {
                                    dto.setQuoteMessageSenderName(quotedMessageSender.getUserName());
                                }
                            }
                           }
                        
                        // 根据被引用消息的类型设置不同的内容
                        if ("image".equals(quotedMessage.getType())) {
                            dto.setQuoteMessageContent("[图片]");
                            dto.setQuoteMessageType("image");
                            
                            // 设置图片URL
                            if (quotedMessage.getImageUrls() != null && !quotedMessage.getImageUrls().isEmpty()) {
                                String fullUrl = ossUtil.convertToFullUrl(quotedMessage.getImageUrls());
                                dto.setQuoteMessageImageUrl(fullUrl);
                            }
                        } else if ("file".equals(quotedMessage.getType())) {
                            dto.setQuoteMessageContent("[文件] " + (quotedMessage.getFileName() != null ? quotedMessage.getFileName() : ""));
                            dto.setQuoteMessageType("file");
                            
                            // 设置文件相关信息
                            if (quotedMessage.getFileUrls() != null && !quotedMessage.getFileUrls().isEmpty()) {
                                String fullUrl = ossUtil.convertToFullUrl(quotedMessage.getFileUrls());
                                dto.setQuoteMessageFileUrl(fullUrl);
                            }
                            dto.setQuoteMessageFileName(quotedMessage.getFileName());
                            if (quotedMessage.getFileSize() != null) {
                                dto.setQuoteMessageFileSize(quotedMessage.getFileSize());
                            }
                        } else {
                            // 默认文本消息
                            dto.setQuoteMessageContent(quotedMessage.getTextContent());
                            dto.setQuoteMessageType("text");
                         }
                    }
                }
                
                dto.setServerTimestamp(message.getServerTimestamp());
                dto.setClientTimestamp(message.getClientTimestamp());
                dto.setCreatedAt(new Date(message.getServerTimestamp()));
                dto.setSenderName(sender != null ? sender.getUserName() : "未知用户");
                
                // 检查是否需要显示日期
                if (message.getServerTimestamp() != null) {
                    LocalDate messageDate = new Date(message.getServerTimestamp()).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                    
                    // 如果这是当天的第一条消息，则标记显示日期
                    if (!datesWithShownDate.contains(messageDate)) {
                        dto.setShowDate(true);
                        datesWithShownDate.add(messageDate);
                    } else {
                        dto.setShowDate(false);
                    }
                }
                
                messageDTOs.add(dto);
            }
            
            return messageDTOs;
        } catch (Exception e) {
             e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    @Override
    public Map<String, Integer> getUnreadMessageCounts(String userId, List<String> friendIds) {
        Map<String, Integer> unreadCounts = new HashMap<>();
        
        try {
            for (String friendId : friendIds) {
                int count = messageMapper.getUnreadMessageCountFromFriend(userId, friendId);
                unreadCounts.put(friendId, count);
            }
        } catch (Exception e) {
              e.printStackTrace();
        }
        
        return unreadCounts;
    }

    @Override
    public void markMessagesAsRead(String userId, String friendId) {
        try {
            // 只更新来自特定好友的消息
            QueryWrapper<Message> messageWrapper = new QueryWrapper<>();
            messageWrapper.eq("from_user_id", friendId)
                      .eq("to_target_id", userId);
            List<Message> messages = messageMapper.selectList(messageWrapper);
        
            if (!messages.isEmpty()) {
                List<String> messageIds = messages.stream()
                .map(Message::getMessageId)
                .collect(Collectors.toList());

                // 更新消息状态为已读
                Message updateMessage = new Message();
                updateMessage.setStatus("read");
                updateMessage.setUpdateTime(new Date());
                
                QueryWrapper<Message> finalWrapper = new QueryWrapper<>();
                finalWrapper.eq("to_target_id", userId)
                        .in("message_id", messageIds)
                        .ne("status", "read"); // 只更新状态不是已读的消息
            
                messageMapper.update(updateMessage, finalWrapper);
            }
        } catch (Exception e) {
              e.printStackTrace();
        }
    }
    
    /**
     * 检查是否是当天第一条消息
     * 
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @return 是否是当天第一条消息
     */
    private boolean isFirstMessageToday(String senderId, String receiverId) {
        try {
            // 获取今天的开始时间
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            // 查询今天两人之间的消息数量
            QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
            queryWrapper.and(wrapper -> wrapper.eq("from_user_id", senderId).eq("to_target_id", receiverId)
                    .or().eq("from_user_id", receiverId).eq("to_target_id", senderId));
            queryWrapper.ge("server_timestamp", today.getTimeInMillis());
            
            int count = messageMapper.selectCount(queryWrapper).intValue();
            
            // 如果这是第一条消息，则返回true
            return count <= 1;
        } catch (Exception e) {
               return false;
        }
    }
    
    /**
     * 根据消息ID查询消息
     * 
     * @param messageId 消息ID
     * @return 消息对象
     */
    public Message getMessageById(String messageId) {
        QueryWrapper<Message> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("message_id", messageId);
        return messageMapper.selectOne(queryWrapper);
    }
}