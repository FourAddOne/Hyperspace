package com.lihuahua.hyperspace.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WebSocketInterceptor implements HandshakeInterceptor {
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    /**
     * 在握手之前执行，可以用来验证用户身份、设置属性等
     *
     * @param request    当前的HTTP请求
     * @param response   当前的HTTP响应
     * @param wsHandler  WebSocket处理器
     * @param attributes 与WebSocket会话关联的属性
     * @return boolean 是否继续握手过程
     * @throws Exception 异常
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        log.info("开始WebSocket握手过程");
        
        // 如果是Servlet请求，可以从中获取一些信息
        if (request instanceof ServletServerHttpRequest servletRequest) {
            // 从请求参数中获取token
            String token = servletRequest.getServletRequest().getParameter("token");
            log.debug("从请求中获取到token: {}", token != null ? "存在" : "不存在");
            
            // 从URL路径中获取用户ID
            String path = servletRequest.getServletRequest().getRequestURI();
            String userId = null;
            
            // 解析路径中的用户ID，例如 /ws/1000000000
            if (path != null && path.startsWith("/ws/")) {
                String[] parts = path.split("/");
                if (parts.length >= 3) {
                    userId = parts[2];
                    log.debug("从URL路径中解析出用户ID: {}", userId);
                }
            }
            
            // 如果URL中没有用户ID，尝试从token中获取
            if (userId == null || userId.isEmpty()) {
                if (token != null && !token.isEmpty()) {
                    try {
                        log.debug("开始解析和验证token");
                        // 解析token获取用户信息
                        userId = jwtTokenUtil.getUserIdFromToken(token);
                        log.debug("从token中解析出用户ID: {}", userId);
                    } catch (Exception e) {
                        log.error("解析token时发生错误", e);
                    }
                }
            }
            
            // 验证用户ID
            if (userId != null && !userId.isEmpty()) {
                // 如果提供了token，验证token是否有效
                if (token != null && !token.isEmpty()) {
                    boolean isValid = jwtTokenUtil.validateToken(token);
                    log.debug("token验证结果: {}", isValid ? "有效" : "无效");
                    
                    if (!isValid) {
                        log.warn("提供的token无效，拒绝WebSocket连接");
                        return false;
                    }
                }
                
                // 将用户信息添加到WebSocket会话属性中
                attributes.put("userId", userId);
                if (token != null && !token.isEmpty()) {
                    attributes.put("token", token);
                }
                log.info("WebSocket握手认证成功 - 用户ID: {}", userId);
                return true;
            } else {
                log.warn("无法获取有效的用户ID，拒绝WebSocket连接");
                return false;
            }
        } else {
            log.warn("非Servlet请求，拒绝WebSocket连接");
            return false;
        }
    }
    
    /**
     * 在握手之后执行，可以用来记录日志或其他后处理操作
     *
     * @param request   当前的HTTP请求
     * @param response  当前的HTTP响应
     * @param wsHandler WebSocket处理器
     * @param exception 握手过程中可能发生的异常
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                              WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手完成后发生异常", exception);
        } else {
            log.info("WebSocket握手过程完成");
        }
    }
}