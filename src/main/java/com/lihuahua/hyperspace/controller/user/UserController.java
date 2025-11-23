package com.lihuahua.hyperspace.controller.user;

import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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

    // 获取用户信息
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

    // 更新头像
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