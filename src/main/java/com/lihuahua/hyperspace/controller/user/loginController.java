package com.lihuahua.hyperspace.controller.user;

import com.lihuahua.hyperspace.enums.LoginFail;
import com.lihuahua.hyperspace.exception.LoginException;
import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Tag(name = "用户登录接口")
@RestController
@RequestMapping("/user")
public class loginController {

    @Resource
    private UserServer userServer;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ResVO<UserLoginVO> login(@RequestBody LoginDTO loginUser) {
        Map<String, String> credential = new HashMap<>();
        credential.put("userId", loginUser.getUserId());
        credential.put("email", loginUser.getEmail());
        credential.put("password", loginUser.getPassword());
        credential.put("ip", loginUser.getIp());

        try {
            UserLoginVO resUser = userServer.login(credential);
            // 确保返回的accessToken不为null
            if (resUser.getAccessToken() == null || resUser.getAccessToken().isEmpty()) {
                return ResVO.fail("登录失败：无法生成访问令牌");
            }
            return ResVO.success(resUser);
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }

    @Operation(summary = "用户注销")
    @PostMapping("/logout")
    public ResVO<String> logout(@RequestHeader("Authorization") String token) {
        try {
            // 移除 "Bearer " 前缀
            String jwt = token.replace("Bearer ", "");
            
            // 验证并获取用户ID
            String userId = JwtTokenUtil.validateToken(jwt);
            
            // 即使token无效，我们也应该清除本地状态
            if (userId != null) {
                // 如果用户ID有效，则执行登出逻辑
                Boolean result = userServer.logout(userId);
                if (result) {
                    // 从Redis中移除token
                    JwtTokenUtil.removeTokenFromRedis(userId);
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

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public ResVO<UserLoginVO> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            String userId = JwtTokenUtil.validateToken(token.replace("Bearer ", ""));
            if (userId != null) {
                UserLoginVO userInfo = userServer.getUserInfo(userId);
                return ResVO.success(userInfo);
            }
            return ResVO.fail("无效的token");
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }

    @Operation(summary = "更新头像")
    @PostMapping("/avatar")
    public ResVO<String> updateAvatar(@RequestHeader("Authorization") String token,
                                       @RequestParam("avatarUrl") String avatarUrl) {
        try {
            System.out.println("开始更新头像，token: " + token + ", avatarUrl: " + avatarUrl);
            String userId = JwtTokenUtil.validateToken(token.replace("Bearer ", ""));
            System.out.println("解析出的用户ID: " + userId);
            if (userId != null) {
                Boolean result = userServer.updateAvatar(userId, avatarUrl);
                System.out.println("更新头像结果: " + result);
                if (result) {
                    return ResVO.success("头像更新成功");
                }
            }
            return ResVO.fail("头像更新失败");
        } catch (Exception e) {
            System.err.println("更新头像异常: " + e.getMessage());
            e.printStackTrace();
            return ResVO.fail(e.getMessage());
        }
    }
}