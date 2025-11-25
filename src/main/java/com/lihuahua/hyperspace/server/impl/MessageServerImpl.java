package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.mapper.MessageMapper;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.models.dto.MessageDTO;
import com.lihuahua.hyperspace.models.entity.Message;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.server.MessageServer;
import com.lihuahua.hyperspace.utils.SnowflakeIdUtil;
import com.lihuahua.hyperspace.utils.OssUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.beans.BeansException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.Calendar;
import java.util.Comparator;
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
        messageMapper.insert(message);
        return true;
    }

    @Override
    public boolean sendMessage(MessageDTO messageDTO) {
        try {
            Message message = new Message();
            // 生成业务主键
            message.setMessageId("msg_" + java.util.UUID.randomUUID().toString().replace("-", ""));
            message.setType(messageDTO.getType() != null ? messageDTO.getType() : "text");
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
            
            message.setClientTimestamp(messageDTO.getClientTimestamp() != null ? messageDTO.getClientTimestamp() : System.currentTimeMillis());
            message.setServerTimestamp(System.currentTimeMillis());
            message.setStatus("success");
            message.setCreateTime(new Date());
            message.setUpdateTime(new Date());
            
            System.out.println("准备插入消息: " + message);
            
            // 使用自定义插入方法
            int result = messageMapper.insertMessage(message);
            
            System.out.println("消息插入结果: " + result);
            
            // 更新messageDTO的ID
            messageDTO.setMessageId(message.getMessageId());
            messageDTO.setServerTimestamp(message.getServerTimestamp());
            messageDTO.setCreatedAt(new Date(message.getServerTimestamp()));
            
            // 检查是否是当天第一条消息
            messageDTO.setShowDate(isFirstMessageToday(message.getFromUserId(), message.getToTargetId()));
            
            return result > 0;
        } catch (Exception e) {
            System.err.println("发送消息时出错: " + e.getMessage());
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
            System.err.println("获取聊天历史时出错: " + e.getMessage());
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
            System.err.println("获取未读消息数量时出错: " + e.getMessage());
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
            System.err.println("标记消息为已读时出错: " + e.getMessage());
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
            System.err.println("检查是否是当天第一条消息时出错: " + e.getMessage());
            return false;
        }
    }
}