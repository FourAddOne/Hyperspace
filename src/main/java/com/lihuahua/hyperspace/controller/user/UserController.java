package com.lihuahua.hyperspace.controller.user;

import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userController")
@RequestMapping("/user")
public class UserController {
    
    @Resource
    private UserServer userServer;
    
    // 搜索用户
    @GetMapping("/search")
    public ResVO<List<UserLoginVO>> searchUsers(@RequestParam String keyword, HttpServletRequest request) {
        try {
            // 从请求属性中获取当前用户ID
            String currentUserId = (String) request.getAttribute("userId");
            if (currentUserId == null || currentUserId.isEmpty()) {
                // 如果从请求属性中获取不到，则尝试从JWT token中获取
                currentUserId = JwtTokenUtil.getCurrentUserId();
            }
            
            List<UserLoginVO> users = userServer.searchUsers(keyword, currentUserId);
            return ResVO.success(users);
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
    
    // 获取用户信息（用于测试）
    @GetMapping("/test-info")
    public ResVO<UserLoginVO> getUserInfoForTest(@RequestParam String userId) {
        try {
            UserLoginVO userInfo = userServer.getUserInfo(userId);
            System.out.println("获取用户信息测试 - 用户ID: " + userId + ", 用户信息: " + userInfo);
            return ResVO.success(userInfo);
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
}