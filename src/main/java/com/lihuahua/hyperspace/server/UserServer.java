package com.lihuahua.hyperspace.server;

import com.lihuahua.hyperspace.models.vo.*;
import java.util.List;
import java.util.Map;

public interface UserServer {
    
    // 登录
    AuthVO login(Map<String, String> credential);
    
    // 注册
    Boolean register(Map<String, String> credential);
    
    // 登出
    Boolean logout(String userId);
    
    // 获取用户信息
    UserProfileVO getUserInfo(String userId);
    
    // 更新头像
    Boolean updateAvatar(String userId, String newAvatarUrl);
    
    // 获取用户设置
    UserSettingsVO getUserSettings(String userId);
    
    // 保存用户设置
    Boolean saveUserSettings(UserSettingsVO userSettingsVO);
    
    // 搜索用户（不包含屏蔽的用户）
    List<UserBasicVO> searchUsers(String keyword, String userId);
    
    // 搜索用户
    List<UserBasicVO> searchUsers(String keyword);

    // 更新用户在线状态
    boolean updateUserStatus(String userId, boolean isOnline);
}