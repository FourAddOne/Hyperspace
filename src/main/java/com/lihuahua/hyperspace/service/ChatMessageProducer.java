package com.lihuahua.hyperspace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.config.RabbitMQChatConfig;
import com.lihuahua.hyperspace.models.dto.MessageDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 发送群聊消息到RabbitMQ
     * @param messageDTO 消息对象
     */
    public void sendGroupMessage(MessageDTO messageDTO) {
        try {
            String messageJson = objectMapper.writeValueAsString(messageDTO);
            rabbitTemplate.convertAndSend(
                RabbitMQChatConfig.CHAT_GROUP_EXCHANGE,
                RabbitMQChatConfig.CHAT_GROUP_ROUTING_KEY,
                messageJson
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}