package com.lihuahua.hyperspace.controller.user;

import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.vo.AuthVO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.models.vo.UserProfileVO;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户注册接口")
@RestController
@RequestMapping("/user")
public class RegisterController {

    @Resource
    private UserServer userServer;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ResVO<Map<String, Object>> register(@RequestBody RegisterDTO user) {
        Map<String, String> credential = new HashMap<>();
        credential.put("username", user.getUserName()); // 使用 userName 字段
        credential.put("password", user.getPassword());
        credential.put("email", user.getEmail());
        credential.put("ip", user.getIp());

        try {
            Boolean result = userServer.register(credential);
            if (result) {
                // 注册成功后自动登录
                Map<String, String> loginCredential = new HashMap<>();
                loginCredential.put("email", user.getEmail());
                loginCredential.put("password", user.getPassword());
                loginCredential.put("ip", user.getIp());
                
                AuthVO authVO = userServer.login(loginCredential);
                UserProfileVO userProfileVO = userServer.getUserInfo(authVO.getUserId());
                
                // 添加缺失的refreshToken
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("accessToken", authVO.getAccessToken());
                responseData.put("refreshToken", authVO.getRefreshToken()); // 添加refreshToken
                responseData.put("userId", userProfileVO.getUserId());
                responseData.put("userName", userProfileVO.getUserName());
                responseData.put("email", userProfileVO.getEmail());
                responseData.put("avatarUrl", userProfileVO.getAvatarUrl());
                responseData.put("Ip", userProfileVO.getIp());
                
                return ResVO.success(responseData);
            } else {
                return ResVO.fail("注册失败");
            }
        } catch (Exception e) {
            e.printStackTrace(); // 添加错误日志
            return ResVO.fail(e.getMessage());
        }
    }
}