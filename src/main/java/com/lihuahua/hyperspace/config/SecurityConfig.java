package com.lihuahua.hyperspace.config;

import com.lihuahua.hyperspace.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 公开接口
                        .requestMatchers("/user/login", "/user/register").permitAll()
                        // 2. WebSocket端点
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/groups/**").permitAll()
                        // 3. 静态资源
                        .requestMatchers("/uploads/**").permitAll()
                        // 4. 文件上传接口
                        .requestMatchers("/file/upload").permitAll()
                        // 5. OSS相关接口
                        .requestMatchers("/oss/**").authenticated()
                        // 6. Knife4j/Swagger 接口文档放行（补充完整路径）
                        .requestMatchers(
                                "/doc.html",                    // Knife4j 接口文档首页
                                "/swagger-ui.html",             // 兼容 Swagger UI 路径
                                "/swagger-ui/**",               // Swagger UI 静态资源
                                "/v3/api-docs/**",              // OpenAPI 3.0 接口文档数据接口
                                "/v2/api-docs",                 // Swagger 2.0 接口文档数据接口（关键补充）
                                "/webjars/**",                  // 文档依赖的静态资源（如JS/CSS）
                                "/swagger-resources/**",        // Swagger 资源配置
                                "/swagger-resources/configuration/ui",    // UI配置
                                "/swagger-resources/configuration/security" // 安全配置
                        ).permitAll()
                        // 7. 其他接口需要认证
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}