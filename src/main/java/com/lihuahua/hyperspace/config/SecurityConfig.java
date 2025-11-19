package com.lihuahua.hyperspace.config;

import com.lihuahua.hyperspace.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
            // 放行 Knife4j 相关路径
            .requestMatchers("/doc.html").permitAll()
            .requestMatchers("/webjars/**").permitAll()
            .requestMatchers("/swagger-resources/**").permitAll()
            .requestMatchers("/v3/api-docs/**").permitAll()
            .requestMatchers("/v2/api-docs/**").permitAll()
            .requestMatchers("/swagger-ui/**").permitAll()
            .requestMatchers("/user/login").permitAll()
            .requestMatchers("/user/register").permitAll()
            .requestMatchers("/").permitAll()
            .requestMatchers("/login").permitAll()
            .requestMatchers("/register").permitAll()
            .requestMatchers("/static/**").permitAll()
            .requestMatchers("/*.html").permitAll()
            .requestMatchers("/favicon.ico").permitAll()
            .requestMatchers("/ws/**").permitAll() // 放行WebSocket端点
            .requestMatchers("/sockjs/**").permitAll() // 放行SockJS端点
            .requestMatchers("/friend/**").permitAll() // 放行好友相关API

            // 其他配置...
            .anyRequest().authenticated()
        )
            .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/api/**", "/user/login", "/user/register", "/", "/login", "/register", "/static/**", "/favicon.ico", "/index.html", "/ws/**", "/sockjs/**", "/friend/**")
            )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}