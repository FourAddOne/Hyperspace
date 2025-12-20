package com.lihuahua.hyperspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.config.RabbitMQChatConfig;
import com.lihuahua.hyperspace.models.dto.MessageDTO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.server.MessageServer;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.utils.OssUtil;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageConsumer {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MessageServer messageServer;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private OssUtil ossUtil;
    
    /**
     * 监听群聊消息队列并处理消息
     * @param messageJson 消息JSON字符串
     */
    @RabbitListener(queues = RabbitMQChatConfig.CHAT_GROUP_QUEUE)
    public void handleGroupMessage(String messageJson) {
        try {
            // 将JSON字符串转换为MessageDTO对象
            MessageDTO messageDTO = objectMapper.readValue(messageJson, MessageDTO.class);
            
            // 设置目标类型为group
            messageDTO.setToTargetType("group");
            
            // 保存消息到数据库
            boolean saved = messageServer.sendGroupMessage(messageDTO);
            
            if (saved) {
                System.out.println("群聊消息已保存到数据库: " + messageDTO.getMessageId());
                
                // 获取发送者信息并添加到消息中
                User sender = userMapper.selectById(messageDTO.getFromUserId());
                if (sender != null) {
                    messageDTO.setFromUsername(sender.getUserName());
                    // 设置用户头像URL（转换为完整URL）
                    if (sender.getAvatarUrl() != null && !sender.getAvatarUrl().isEmpty()) {
                        messageDTO.setUserAvatarUrl(ossUtil.convertToFullUrl(sender.getAvatarUrl()));
                    }
                    
                    // 处理图片消息，将相对路径转换为完整URL
                    if (messageDTO.getImageUrls() != null && !messageDTO.getImageUrls().isEmpty()) {
                        messageDTO.setImageUrls(ossUtil.convertToFullUrl(messageDTO.getImageUrls()));
                    }
                }
                
                // 设置服务器时间戳
                messageDTO.setServerTimestamp(System.currentTimeMillis());
                
                // 广播消息到群组
                messagingTemplate.convertAndSend(
                    "/topic/group/" + messageDTO.getToTargetId(),
                    messageDTO
                );
            } else {
                System.err.println("保存群聊消息到数据库失败: " + messageDTO.getMessageId());
            }
        } catch (Exception e) {
            System.err.println("处理群聊消息时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}