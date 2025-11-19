package com.lihuahua.hyperspace.rabbitmq.consumer;

import com.lihuahua.hyperspace.rabbitmq.producer.dto.ChatMessageDTO;
import com.lihuahua.hyperspace.websocket.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Message;

/**
 * 私聊消息消费者
 * 监听用户的私聊消息队列并处理消息
 */
@Component
public class PrivateMessageConsumer implements MessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(PrivateMessageConsumer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 监听并处理私聊消息
     * 注意：在实际应用中，每个用户都需要监听自己的队列
     * 这里我们使用通配符绑定来监听所有私聊队列
     * @param message 接收到的聊天消息
     */
    @RabbitListener(queues = "#{@rabbitListenerConfig.privateQueueName}")
    public void handlePrivateMessage(ChatMessageDTO message) {
        logger.info("收到私聊消息: messageId={}, fromUserId={}, toTargetId={}, textContent={}", 
                   message.getMessageId(), message.getFromUserId(), message.getToTargetId(), message.getTextContent());
        
        try {
            // 通过WebSocket将消息推送到前端
            String targetUserId = message.getToTargetId();
            // 将消息转换为JSON格式发送
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);
            ChatWebSocketHandler.sendMessageToUser(targetUserId, textMessage);
            logger.info("已将消息推送到用户 {}: {}", targetUserId, message.getTextContent());
        } catch (Exception e) {
            logger.error("推送消息到用户时出错: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onMessage(Message message) {
        try {
            // 将AMQP消息转换为ChatMessageDTO对象
            ChatMessageDTO chatMessage = objectMapper.readValue(message.getBody(), ChatMessageDTO.class);
            // 处理消息
            handlePrivateMessage(chatMessage);
        } catch (Exception e) {
            logger.error("处理RabbitMQ消息时出错: " + e.getMessage(), e);
        }
    }
}