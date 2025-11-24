package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.mapper.MessageMapper;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.mapper.MessageStatusMapper;
import com.lihuahua.hyperspace.models.dto.MessageDTO;
import com.lihuahua.hyperspace.models.entity.Message;
import com.lihuahua.hyperspace.models.entity.MessageStatus;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.server.MessageServer;
import com.lihuahua.hyperspace.utils.SnowflakeIdUtil;
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
    private MessageStatusMapper messageStatusMapper;
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean sendGroupMessage(MessageDTO messageDTO) {
        Message message = new Message();
        BeanUtils.copyProperties(messageDTO, message);
        message.setMessageId(SnowflakeIdUtil.nextId());
        message.setContentType("text");
        message.setStatus("sent");
        message.setTimestamp(new Date());
        messageMapper.insert(message);
        return true;

    }

    @Override
    public boolean sendMessage(MessageDTO messageDTO) {
        try {
            Message message = new Message();
            message.setMessageId(SnowflakeIdUtil.nextId());
            message.setSenderId(messageDTO.getSenderId()); // 确保senderId正确设置
            message.setReceiverId(messageDTO.getReceiverId());
            message.setContent(messageDTO.getContent());
            message.setTimestamp(new Date());
            message.setStatus("sent");
            message.setContentType("text"); // 设置默认内容类型
            
            // 为私人聊天生成conversationId（基于senderId和receiverId的组合）
            String conversationId = generatePrivateConversationId(messageDTO.getSenderId(), messageDTO.getReceiverId());
            message.setConversationId(conversationId);
            
            System.out.println("准备插入消息: " + message);
            
            // 确保会话存在
            ensureConversationExists(conversationId, messageDTO.getSenderId(), messageDTO.getReceiverId());
            
            // 使用自定义插入方法
            int result = messageMapper.insertMessage(message);
            
            System.out.println("消息插入结果: " + result);
            
            // 创建消息状态记录
            if (result > 0) {
                MessageStatus senderStatus = new MessageStatus();
                senderStatus.setMessageId(message.getMessageId());
                senderStatus.setUserId(message.getSenderId());
                senderStatus.setStatus("read"); // 发送者的消息状态设为已读
                senderStatus.setUpdatedAt(new Date());
                messageStatusMapper.insert(senderStatus);
                
                MessageStatus receiverStatus = new MessageStatus();
                receiverStatus.setMessageId(message.getMessageId());
                receiverStatus.setUserId(message.getReceiverId());
                receiverStatus.setStatus("delivered"); // 接收者的消息状态设为已送达
                receiverStatus.setUpdatedAt(new Date());
                messageStatusMapper.insert(receiverStatus);
            }
            
            // 更新messageDTO的ID
            messageDTO.setId(message.getMessageId());
            messageDTO.setCreatedAt(message.getTimestamp());
            
            // 检查是否是当天第一条消息
            messageDTO.setShowDate(isFirstMessageToday(message.getSenderId(), message.getReceiverId()));
            
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
            
            // 用于跟踪每天是否已经显示过日期
            Set<LocalDate> datesWithShownDate = new HashSet<>();
            
            // 按时间排序消息
            messages.sort(Comparator.comparing(Message::getTimestamp));
            
            for (Message message : messages) {
                User sender = userMapper.selectById(message.getSenderId());
                MessageDTO dto = new MessageDTO();
                dto.setId(message.getMessageId());
                dto.setSenderId(message.getSenderId());
                dto.setReceiverId(message.getReceiverId());
                dto.setContent(message.getContent());
                dto.setCreatedAt(message.getTimestamp());
                dto.setSenderName(sender != null ? sender.getUserName() : "未知用户");
                
                // 检查是否需要显示日期
                if (message.getTimestamp() != null) {
                    LocalDate messageDate = message.getTimestamp().toInstant()
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
            messageWrapper.eq("sender_id", friendId)
                      .eq("receiver_id", userId);
            List<Message> messages = messageMapper.selectList(messageWrapper);
        
            if (!messages.isEmpty()) {
                List<String> messageIds = messages.stream()
                .map(Message::getMessageId)
                .collect(Collectors.toList());
            
                MessageStatus updateStatus = new MessageStatus();
                updateStatus.setStatus("read");
                updateStatus.setUpdatedAt(new Date());
            
                QueryWrapper<MessageStatus> finalWrapper = new QueryWrapper<>();
                finalWrapper.eq("user_id", userId)
                        .in("message_id", messageIds)
                        .eq("status", "delivered"); // 只更新状态为已送达的消息
            
                messageStatusMapper.update(updateStatus, finalWrapper);
            }
        } catch (Exception e) {
            System.err.println("标记消息为已读时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 为私人聊天生成唯一的会话ID
     * 通过组合两个用户ID并排序来确保同一对用户始终生成相同的会话ID
     * 
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @return 唯一的会话ID
     */
    private String generatePrivateConversationId(String userId1, String userId2) {
        // 确保两个用户ID按字典序排列，这样无论谁是发送方谁是接收方，都会生成相同的会话ID
        String sortedId1 = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        String sortedId2 = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        
        // 使用哈希方式创建较短的会话ID
        String combinedIds = sortedId1 + sortedId2;
        return String.valueOf(Math.abs(combinedIds.hashCode())) + String.valueOf(Math.abs((combinedIds + "_alt").hashCode()));
    }
    
    /**
     * 确保会话存在，如果不存在则创建
     * 
     * @param conversationId 会话ID
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     */
    private void ensureConversationExists(String conversationId, String senderId, String receiverId) {
        // 检查会话是否已存在
        try {
            // 这里应该调用ConversationMapper来检查会话是否存在
            // 为简化实现，我们直接尝试插入，如果违反主键约束则说明已存在
            String sql = "INSERT IGNORE INTO conversations (conversation_id, type, created_at, updated_at) VALUES (?, 'private', NOW(), NOW())";
            
            // 使用JdbcTemplate执行SQL
            // 注意：实际项目中应该使用Mapper或者Repository模式
            DataSource dataSource = applicationContext.getBean(DataSource.class);
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, conversationId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                // 忽略异常，因为可能违反主键约束
                System.out.println("会话可能已存在或创建失败: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("检查会话存在性时出错: " + e.getMessage());
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
            queryWrapper.and(wrapper -> wrapper.eq("sender_id", senderId).eq("receiver_id", receiverId)
                    .or().eq("sender_id", receiverId).eq("receiver_id", senderId));
            queryWrapper.ge("timestamp", today.getTime());
            
            int count = messageMapper.selectCount(queryWrapper).intValue();
            
            // 如果这是第一条消息，则返回true
            return count <= 1;
        } catch (Exception e) {
            System.err.println("检查是否是当天第一条消息时出错: " + e.getMessage());
            return false;
        }
    }
}