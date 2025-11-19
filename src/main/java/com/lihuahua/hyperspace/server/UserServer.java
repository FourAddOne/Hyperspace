package com.lihuahua.hyperspace.server;

import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.entity.UserSettings;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.models.vo.UserSettingsVO;

import java.util.List;
import java.util.Map;

public interface UserServer {

    UserLoginVO login(Map<String, String> credential);

    Boolean register(Map<String, String> credential);

    Boolean logout(String userId);

    UserLoginVO getUserInfo(String userId);

    Boolean updateAvatar(String userId, String newAvatarUrl);
    
    UserSettingsVO getUserSettings(String userId);
    
    Boolean saveUserSettings(UserSettingsVO userSettings);
    
    // 搜索用户
    List<UserLoginVO> searchUsers(String keyword);
    
    // 搜索用户（排除指定用户）
    List<UserLoginVO> searchUsers(String keyword, String excludeUserId);
    
    // 检查用户是否为好友
    boolean isFriend(String userId, String friendId);
    
    // 更新用户在线状态
    boolean updateUserStatus(String userId, boolean isOnline);
}