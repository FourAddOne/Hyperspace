package com.lihuahua.hyperspace.controller.user;
import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.vo.AuthVO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.models.vo.UserProfileVO;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Tag(name = "用户登录接口")
@RestController
@RequestMapping("/user")
public class loginController {

    private static final Logger logger = LoggerFactory.getLogger(loginController.class);

    @Resource
    private UserServer userServer;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ResVO<Map<String, Object>> login(@RequestBody LoginDTO loginUser) {
        Map<String, String> credential = new HashMap<>();
        credential.put("userId", loginUser.getUserId());
        credential.put("email", loginUser.getEmail());
        credential.put("password", loginUser.getPassword());
        credential.put("ip", loginUser.getIp()); // 修复字段名

        try {
            AuthVO authVO = userServer.login(credential);
            // 获取用户信息
            UserProfileVO userProfileVO = userServer.getUserInfo(authVO.getUserId());
            
            // 构建返回数据
            Map<String, Object> resData = new HashMap<>();
            resData.put("accessToken", authVO.getAccessToken());
            resData.put("refreshToken", authVO.getRefreshToken());
            resData.put("userId", userProfileVO.getUserId());
            resData.put("userName", userProfileVO.getUserName());
            resData.put("email", userProfileVO.getEmail());
            resData.put("avatarUrl", userProfileVO.getAvatarUrl());
            resData.put("Ip", userProfileVO.getIp());
            
            // 确保返回的accessToken不为null
            if (authVO.getAccessToken() == null || authVO.getAccessToken().isEmpty()) {
                return ResVO.fail("登录失败：无法生成访问令牌");
            }
            return ResVO.success(resData);
        } catch (Exception e) {
            e.printStackTrace(); // 添加错误日志
            return ResVO.fail(e.getMessage());
        }
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public ResVO<Map<String, String>> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        try {
            logger.info("开始处理令牌刷新请求");
            logger.info("接收到的完整Authorization头部: {}", refreshToken);
            
            // 移除 "Bearer " 前缀
            String token = refreshToken.replace("Bearer ", "");
            logger.info("提取后的令牌: {}", token);
            
            // 使用专用的refreshToken验证方法
            String userId = JwtTokenUtil.validateRefreshToken(token);
            logger.info("验证refreshToken结果，用户ID: {}", userId);
            
            if (userId != null) {
                logger.info("刷新令牌验证成功，用户ID: {}", userId);
                
                // 生成新的访问令牌和刷新令牌
                String newAccessToken = JwtTokenUtil.generateAccessToken(userId);
                String newRefreshToken = JwtTokenUtil.generateRefreshToken(userId);
                logger.info("为用户 {} 生成新的访问令牌和刷新令牌", userId);
                
                // 构建返回数据
                Map<String, String> resData = new HashMap<>();
                resData.put("accessToken", newAccessToken);
                resData.put("refreshToken", newRefreshToken);
                
                logger.info("成功刷新令牌，准备返回响应");
                return ResVO.success(resData);
            } else {
                logger.warn("无效的刷新令牌: {}", token);
                return ResVO.fail("无效的刷新令牌");
            }
        } catch (JwtException e) {
            logger.error("刷新令牌验证失败", e);
            return ResVO.fail("刷新令牌验证失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("刷新令牌时发生错误", e);
            return ResVO.fail("刷新令牌时发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "用户注销")
    @PostMapping("/logout")
    public ResVO<String> logout(@RequestHeader("Authorization") String token) {
        try {
            // 移除 "Bearer " 前缀
            String jwt = token.replace("Bearer ", "");
            
            // 验证并获取用户ID（可以使用任意一种令牌）
            String userId = JwtTokenUtil.validateToken(jwt);
            
            // 即使token无效，我们也应该清除本地状态
            if (userId != null) {
                // 如果用户ID有效，则执行登出逻辑
                Boolean result = userServer.logout(userId);
                if (result) {
                    return ResVO.success("注销成功");
                }
            } else {
                // 如果token无效，仍然返回成功，因为用户已经处于登出状态
                return ResVO.success("注销成功");
            }
            
            return ResVO.fail("注销失败");
        } catch (Exception e) {
            // 即使出现异常，也认为登出成功，确保用户能够登出
            return ResVO.success("注销成功");
        }
    }
}