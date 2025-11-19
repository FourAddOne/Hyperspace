package com.lihuahua.hyperspace.websocket;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.lihuahua.hyperspace.rabbitmq.consumer.PrivateMessageConsumer;
import com.lihuahua.hyperspace.config.RabbitConfig;
import static com.lihuahua.hyperspace.constant.RabbitConstant.*;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    // 存储所有活跃的WebSocket会话
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 根据用户ID存储会话
    private static final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();
    
    // 存储用户的RabbitMQ监听器容器
    private static final ConcurrentHashMap<String, MessageListenerContainer> userListeners = new ConcurrentHashMap<>();
    
    @Autowired
    private RabbitAdmin rabbitAdmin;
    
    @Autowired
    private DirectExchange directExchange;
    
    @Autowired
    private ConnectionFactory connectionFactory;
    
    @Autowired
    private PrivateMessageConsumer privateMessageConsumer;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    /**
     * 建立连接后触发的回调方法
     *
     * @param session 当前建立连接的WebSocket会话
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 将新建立的会话添加到会话集合中
        sessions.put(session.getId(), session);
        
        // 获取用户ID
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.put(userId, session.getId());
            log.info("WebSocket连接已建立 - 用户ID: {}, 会话ID: {}", userId, session.getId());
            session.sendMessage(new TextMessage("欢迎连接到聊天服务器，用户ID: " + userId));
            
            // 为用户创建并启动RabbitMQ监听器
            createAndStartUserListener(userId);
        } else {
            log.info("WebSocket连接已建立 - 匿名用户, 会话ID: {}", session.getId());
            session.sendMessage(new TextMessage("欢迎连接到聊天服务器"));
        }
        
        log.debug("当前活跃连接数: {}", getActiveSessionsCount());
    }
    
    /**
     * 接收到消息后触发的回调方法
     *
     * @param session 当前WebSocket会话
     * @param message 接收到的文本消息
     * @throws Exception 异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String userId = (String) session.getAttributes().get("userId");
        String sessionId = session.getId();
        
        log.info("收到消息 - 用户ID: {}, 会话ID: {}, 消息内容: {}", userId, sessionId, payload);
        
        // 处理特殊命令
        if (payload.startsWith("/online")) {
            // 查询在线用户数
            int onlineCount = getActiveSessionsCount();
            session.sendMessage(new TextMessage("当前在线用户数: " + onlineCount));
            log.debug("用户 {} 查询在线人数: {}", userId, onlineCount);
        } else if (payload.startsWith("/help")) {
            // 显示帮助信息
            StringBuilder helpMessage = new StringBuilder();
            helpMessage.append("可用命令:\n");
            helpMessage.append("/online - 查看在线用户数\n");
            helpMessage.append("/help - 显示帮助信息\n");
            helpMessage.append("其他消息将被回显");
            session.sendMessage(new TextMessage(helpMessage.toString()));
        } else {
            // 简单回显消息
            session.sendMessage(new TextMessage("服务器收到: " + payload));
            log.debug("向用户 {} 回显消息: {}", userId, payload);
        }
    }
    
    /**
     * 传输发生错误时触发的回调方法
     *
     * @param session 当前WebSocket会话
     * @param exception 发生的异常
     * @throws Exception 异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String sessionId = session.getId();
        log.error("WebSocket传输错误 - 用户ID: {}, 会话ID: {}", userId, sessionId, exception);
        // 移除出错的会话
        removeSession(session);
        log.info("已移除出错的会话 - 用户ID: {}, 会话ID: {}", userId, sessionId);
    }
    
    /**
     * 连接关闭后触发的回调方法
     *
     * @param session 当前WebSocket会话
     * @param status 关闭状态
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String sessionId = session.getId();
        // 移除已关闭的会话
        removeSession(session);
        
        // 停止并移除用户的RabbitMQ监听器
        if (userId != null) {
            stopAndRemoveUserListener(userId);
        }
        
        log.info("WebSocket连接已关闭 - 用户ID: {}, 会话ID: {}, 关闭状态: {} (代码: {}, 原因: {})", 
                userId, sessionId, status, status.getCode(), status.getReason());
        log.debug("当前活跃连接数: {}", getActiveSessionsCount());
    }
    
    /**
     * 是否支持部分消息处理
     *
     * @return boolean 是否支持部分消息
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * 获取当前活跃的会话数量
     *
     * @return int 活跃会话数
     */
    public static int getActiveSessionsCount() {
        return sessions.size();
    }
    
    /**
     * 统一移除会话的方法
     * @param session WebSocket会话
     */
    private void removeSession(WebSocketSession session) {
        String sessionId = session.getId();
        String userId = (String) session.getAttributes().get("userId");
        
        sessions.remove(sessionId);
        if (userId != null) {
            userSessions.remove(userId);
        }
        
        log.debug("会话已移除 - 用户ID: {}, 会话ID: {}", userId, sessionId);
    }
    
    /**
     * 根据用户ID发送消息
     * @param userId 用户ID
     * @param message 消息内容
     * @throws Exception 异常
     */
    public static void sendMessageToUser(String userId, TextMessage message) throws Exception {
        String sessionId = userSessions.get(userId);
        if (sessionId != null) {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                session.sendMessage(message);
                log.debug("向用户 {} 发送消息: {}", userId, message.getPayload());
            } else {
                log.warn("无法向用户 {} 发送消息，会话已关闭或不存在", userId);
            }
        } else {
            log.warn("无法向用户 {} 发送消息，未找到对应的会话", userId);
        }
    }
    
    /**
     * 向所有连接的用户广播消息
     * @param message 消息内容
     */
    public static void broadcastMessage(TextMessage message) {
        log.info("广播消息给所有用户: {}", message.getPayload());
        int successCount = 0;
        int failCount = 0;
        
        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
                String userId = (String) session.getAttributes().get("userId");
                log.error("向用户 {} 广播消息时出错", userId, e);
            }
        }
        
        log.info("消息广播完成 - 成功: {}, 失败: {}", successCount, failCount);
    }
    
    /**
     * 为用户创建并启动RabbitMQ监听器
     * @param userId 用户ID
     */
    private void createAndStartUserListener(String userId) {
        // 检查是否已经存在该用户的监听器
        if (userListeners.containsKey(userId)) {
            log.debug("用户 {} 的监听器已存在", userId);
            return;
        }
        
        try {
            // 创建队列（如果不存在）
            String queueName = RabbitConfig.getPrivateQueueName(userId);
            Queue queue = QueueBuilder.durable(queueName)
                    .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                    .build();
            rabbitAdmin.declareQueue(queue);
            
            // 创建绑定关系
            Binding binding = BindingBuilder.bind(queue).to(directExchange).with(userId);
            rabbitAdmin.declareBinding(binding);
            
            // 创建监听器容器
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
            container.setQueueNames(queueName);
            container.setMessageListener((org.springframework.amqp.core.MessageListener) privateMessageConsumer);
            container.setExclusive(false);
            container.setDefaultRequeueRejected(false);
            
            // 启动监听器
            container.start();
            
            // 存储监听器引用
            userListeners.put(userId, container);
            
            log.info("为用户 {} 创建并启动了RabbitMQ监听器，监听队列: {}", userId, queueName);
        } catch (Exception e) {
            log.error("为用户 {} 创建RabbitMQ监听器时出错", userId, e);
        }
    }
    
    /**
     * 停止并移除用户的RabbitMQ监听器
     * @param userId 用户ID
     */
    private void stopAndRemoveUserListener(String userId) {
        MessageListenerContainer container = userListeners.get(userId);
        if (container != null) {
            try {
                container.stop();
                userListeners.remove(userId);
                log.info("已停止并移除用户 {} 的RabbitMQ监听器", userId);
            } catch (Exception e) {
                log.error("停止用户 {} 的RabbitMQ监听器时出错", userId, e);
            }
        }
    }
}