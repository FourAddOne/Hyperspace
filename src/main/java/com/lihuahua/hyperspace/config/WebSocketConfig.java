package com.lihuahua.hyperspace.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.lihuahua.hyperspace.websocket.ChatWebSocketHandler;
import com.lihuahua.hyperspace.websocket.WebSocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSocketInterceptor webSocketInterceptor;
    
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 1. 定义 WebSocket 连接端点（客户端连接的 URL）
        // 例如：客户端通过 ws://localhost:8080/ws/{userId} 连接
        registry.addHandler(chatWebSocketHandler, "/ws/{userId}")
                // 2. 添加拦截器（可选，用于连接前的认证、参数处理）
                .addInterceptors(webSocketInterceptor)
                // 3. 允许跨域（开发环境，生产环境需限制域名）
                .setAllowedOrigins("*");

        // 可选：支持 SockJS（兼容不支持 WebSocket 的浏览器，通过 HTTP 模拟）
        registry.addHandler(chatWebSocketHandler, "/sockjs/{userId}")
                .addInterceptors(webSocketInterceptor)
                .setAllowedOrigins("*")
                .withSockJS();
    }
}