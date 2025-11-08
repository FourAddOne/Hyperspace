package com.lihuahua.hyperspace.filter;

import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器，继承自OncePerRequestFilter确保每个请求只过滤一次
 */

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
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
        // 从请求头中获取Token（格式：Bearer <token>）
        String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = JwtTokenUtil.getUserIdFromToken(jwt);
            } catch (Exception e) {
                // Token无效或过期
                logger.error("无法解析Token: " + e.getMessage());
            }
        }

        // 如果Token有效且未设置认证信息
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (JwtTokenUtil.validateToken(jwt)) {
                // 设置认证信息到上下文
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, null); // 可根据需求添加权限信息
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}