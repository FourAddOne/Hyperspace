package com.lihuahua.hyperspace.filter;

import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT认证过滤器，继承自OncePerRequestFilter确保每个请求只过滤一次
 */

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter implements ChannelInterceptor {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 公开接口路径，不需要JWT验证
    private static final String[] PUBLIC_PATHS = {
        "/user/login",
        "/user/register",
        "/user/refresh"  // 刷新令牌接口也不需要认证
    };
    
    /**
     * 核心过滤方法，用于处理每个请求的认证逻辑
     * @param request 当前HTTP请求对象
     * @param response 当前HTTP响应对象
     * @param filterChain 过滤器链，用于传递请求和响应
     * @throws ServletException 可能抛出的Servlet异常
     * @throws IOException 可能抛出的IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException
    {
        // 检查是否为公开接口路径，如果是则直接跳过JWT验证
        String requestURI = request.getRequestURI();
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 从请求头中获取Token（格式：Bearer <token>）
        String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            // 检查JWT格式是否正确（应该包含两个点）
            if (isValidJwtFormat(jwt)) {
                try {
                    username = JwtTokenUtil.getUserIdFromToken(jwt);
                } catch (ExpiredJwtException e) {
                    // Token过期，记录日志但不立即拒绝请求
                    logger.warn("Token已过期: " + e.getMessage());
                } catch (Exception e) {
                    // Token无效
                    logger.error("无法解析Token: " + e.getMessage());
                }
            } else {
                logger.error("无效的JWT格式: " + jwt);
            }
        }

        // 如果Token有效且未设置认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 使用accessToken验证方法
            String validatedUserId = JwtTokenUtil.validateAccessToken(jwt);
            if (validatedUserId != null) {
                // 设置认证信息到上下文
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, null); // 可根据需求添加权限信息
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                // 将用户ID添加到请求属性中，供控制器使用
                request.setAttribute("userId", validatedUserId);
            }
        }

        filterChain.doFilter(request, response);
    }
    
    /**
     * 检查路径是否为公开路径
     * @param requestURI 请求URI
     * @return 是否为公开路径
     */
    private boolean isPublicPath(String requestURI) {
        for (String publicPath : PUBLIC_PATHS) {
            if (pathMatcher.match(publicPath, requestURI)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 验证JWT格式是否正确
     * @param jwt JWT字符串
     * @return 是否为有效的JWT格式
     */
    private boolean isValidJwtFormat(String jwt) {
        return jwt != null && jwt.chars().filter(ch -> ch == '.').count() == 2;
    }
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                String authHeader = authorization.get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String jwt = authHeader.substring(7);
                    // WebSocket连接使用accessToken验证
                    String userId = JwtTokenUtil.validateAccessToken(jwt);
                    if (userId != null) {
                        // 将用户ID设置到StompHeaderAccessor中
                        accessor.setUser(() -> userId);
                    }
                }
            }
        }
        
        return message;
    }
}