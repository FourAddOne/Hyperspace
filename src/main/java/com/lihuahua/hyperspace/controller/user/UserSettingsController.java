package com.lihuahua.hyperspace.controller.user;

import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.models.vo.UserSettingsVO;
import com.lihuahua.hyperspace.server.UserServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户设置接口")
@RestController
@RequestMapping("/user/settings")
public class UserSettingsController {
    
    @Resource
    private UserServer userServer;
    
    @Operation(summary = "获取用户设置")
    @GetMapping
    public ResVO<UserSettingsVO> getUserSettings(@RequestAttribute("userId") String userId) {
        UserSettingsVO userSettings = userServer.getUserSettings(userId);
        return ResVO.success(userSettings);
    }
    
    @Operation(summary = "保存用户设置")
    @PostMapping
    public ResVO<Boolean> saveUserSettings(@RequestAttribute("userId") String userId, 
                                          @RequestBody UserSettingsVO userSettingsVO) {
        userSettingsVO.setUserId(userId);
        Boolean result = userServer.saveUserSettings(userSettingsVO);
        return ResVO.success(result);
    }
    
    @Operation(summary = "删除用户背景图片")
    @DeleteMapping("/background")
    public ResVO<Boolean> deleteBackgroundImage(@RequestAttribute("userId") String userId,
                                               @RequestParam("imageUrl") String imageUrl) {
        // 由于项目不再使用本地文件上传，直接返回成功
        return ResVO.success(true);
    }
}