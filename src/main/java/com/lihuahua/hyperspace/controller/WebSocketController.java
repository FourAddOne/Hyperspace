package com.lihuahua.hyperspace.controller;

import com.lihuahua.hyperspace.models.dto.MessageDTO;
import com.lihuahua.hyperspace.models.entity.Message;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.server.MessageServer;
import com.lihuahua.hyperspace.server.UserServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private UserServer userServer;
    
    @Autowired
    private MessageServer messageServer;
    
    // 用于跟踪用户连接状态
    private static final ConcurrentHashMap<String, Boolean> userConnectionStatus = new ConcurrentHashMap<>();

    /**
     * 处理用户状态变更消息
     */
    @MessageMapping("/user/status")
    public void handleUserStatus(@Payload Map<String, Object> payload, Principal principal) {
        try {
            // 检查principal是否为null
            if (principal == null) {
                System.err.println("无法处理用户状态消息: 未找到认证用户");
                return;
            }
            
            String userId = principal.getName();
            Boolean status = (Boolean) payload.get("status");
            
            if (userId != null && status != null) {
                // 只有当状态真正改变时才更新数据库
                Boolean previousStatus = userConnectionStatus.get(userId);
                if (previousStatus == null || !previousStatus.equals(status)) {
                    userConnectionStatus.put(userId, status);
                    
                    // 更新用户状态
                    boolean updated = userServer.updateUserStatus(userId, status);
                    if (updated) {
                        System.out.println("用户 " + userId + " 状态更新为: " + (status ? "在线" : "离线"));
                    } else {
                        System.out.println("更新用户 " + userId + " 状态失败");
                    }
                    
                    // 广播用户状态变更消息
                    Map<String, Object> message = new HashMap<>();
                    message.put("userId", userId);
                    message.put("status", status);
                    message.put("timestamp", System.currentTimeMillis());
                    
                    // 发送给所有订阅 /topic/user-status 的客户端
                    messagingTemplate.convertAndSend("/topic/user-status", message);
                }
            }
        } catch (Exception e) {
            System.err.println("处理用户状态消息时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @MessageMapping("/user/info")
    public void getUserInfo(Principal principal) {
        try {
            // 检查principal是否为null
            if (principal == null) {
                System.err.println("无法获取用户信息: 未找到认证用户");
                return;
            }
            
            String userId = principal.getName();
            UserLoginVO userInfo = userServer.getUserInfo(userId);
            
            if (userInfo != null) {
                // 发送用户信息给当前用户
                messagingTemplate.convertAndSendToUser(userId, "/queue/user-info", userInfo);
            }
        } catch (Exception e) {
            System.err.println("获取用户信息时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 处理聊天消息
     */
    @MessageMapping("/chat")
    public void handleChatMessage(@Payload MessageDTO messageDTO, Principal principal) {
        try {
            // 检查principal是否为null
            if (principal == null) {
                System.err.println("无法处理聊天消息: 未找到认证用户");
                return;
            }
            
            String senderId = principal.getName();
            messageDTO.setSenderId(senderId);
            messageDTO.setCreatedAt(new Date());
            
            // 保存消息到数据库
            boolean saved = messageServer.sendMessage(messageDTO);
            
            if (saved) {
                System.out.println("消息已保存到数据库");
                
                // 发送消息给发送者（用于同步）
                messagingTemplate.convertAndSendToUser(
                    messageDTO.getSenderId(),
                    "/queue/messages",
                    messageDTO
                );
                
                // 发送消息给接收者
                messagingTemplate.convertAndSendToUser(
                    messageDTO.getReceiverId(), 
                    "/queue/messages", 
                    messageDTO
                );
            } else {
                System.err.println("保存消息到数据库失败");
            }
        } catch (Exception e) {
            System.err.println("处理聊天消息时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 当用户连接时更新其在线状态
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String userId = headerAccessor.getUser().getName();
            System.out.println("用户 " + userId + " 连接到WebSocket");
            
            // 更新用户状态为在线
            userConnectionStatus.put(userId, true);
            userServer.updateUserStatus(userId, true);
            
            // 广播用户在线状态
            Map<String, Object> message = new HashMap<>();
            message.put("userId", userId);
            message.put("status", true);
            message.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/user-status", message);
        } else {
            System.out.println("未认证的用户尝试连接WebSocket");
        }
    }

    /**
     * 当用户断开连接时更新其离线状态
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String userId = headerAccessor.getUser().getName();
            System.out.println("用户 " + userId + " 断开WebSocket连接");
            
            // 更新用户状态为离线
            userConnectionStatus.put(userId, false);
            userServer.updateUserStatus(userId, false);
            
            // 广播用户离线状态
            Map<String, Object> message = new HashMap<>();
            message.put("userId", userId);
            message.put("status", false);
            message.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/user-status", message);
        }
    }
}