package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.mapper.FriendMapper;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.mapper.UserSettingsMapper;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.entity.UserSettings;
import com.lihuahua.hyperspace.models.vo.*;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.IdUtil;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import com.lihuahua.hyperspace.utils.OssProperties;
import com.lihuahua.hyperspace.utils.OssUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.lihuahua.hyperspace.models.entity.Friend;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServerImpl implements UserServer {
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private UserSettingsMapper userSettingsMapper;
    
    @Resource
    private OssUtil ossUtil;
    
    @Resource
    private OssProperties ossProperties;
    
    @Resource
    private FriendMapper friendMapper; // 添加FriendMapper注入
    
    @Override
    public AuthVO login(Map<String, String> credential) {
        String userId = credential.get("userId");
        String email = credential.get("email");
        String password = credential.get("password");
        String ip = credential.get("ip");
         QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (userId != null && !userId.isEmpty()) {
            queryWrapper.eq("user_id", userId);
        } else if (email != null && !email.isEmpty()) {
            queryWrapper.eq("email", email);
        } else {
            System.out.println("错误：未提供用户ID或邮箱");
            throw new RuntimeException("请提供用户ID或邮箱");
        }
        
        // 添加一行强制不使用缓存的查询
        queryWrapper.last("/* MyBatis-Plus 不使用缓存 */");
        User user = userMapper.selectOne(queryWrapper);
        System.out.println("查询到的用户: " + user);
        
        if (user == null) {
            System.out.println("错误：用户不存在");
            throw new RuntimeException("用户不存在");
        }
        
        // 直接使用User类的login方法进行密码验证
         if (!user.login(credential)) {
            System.out.println("错误：密码验证失败");
            throw new RuntimeException("密码错误");
        }
        
        // 更新登录信息
        user.setLoginIp(ip);
        user.setLoginStatus(true); // 设置用户为在线状态
        userMapper.updateById(user);
        // 生成访问令牌和刷新令牌
        String accessToken = JwtTokenUtil.generateAccessToken(user.getUserId());
        String refreshToken = JwtTokenUtil.generateRefreshToken(user.getUserId());
        
        // 构建返回的认证信息
        AuthVO authVO = AuthVO.builder()
                .userId(user.getUserId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return authVO;
    }
    
    /**
     * 生成唯一的用户ID，确保在数据库中不存在重复
     * @return 唯一的11位用户ID
     */
    private String generateUniqueUserId() {
        String userId;
        do {
            userId = IdUtil.generateUserId();
            // 检查数据库中是否已存在该ID
        } while (userMapper.selectById(userId) != null);
        return userId;
    }
    
    @Override
    public Boolean register(Map<String, String> credential) {
        String username = credential.get("username");
        String email = credential.get("email");
        String password = credential.get("password");
        String ip = credential.get("ip");
        
        // 检查邮箱是否已存在
        QueryWrapper<User> emailQueryWrapper = new QueryWrapper<>();
        emailQueryWrapper.eq("email", email);
        if (userMapper.selectCount(emailQueryWrapper) > 0) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 检查用户名是否已存在
        QueryWrapper<User> usernameQueryWrapper = new QueryWrapper<>();
        usernameQueryWrapper.eq("user_name", username);
        if (userMapper.selectCount(usernameQueryWrapper) > 0) {
            throw new RuntimeException("用户名已被使用");
        }
        
        // 创建新用户
        User newUser = new User();
        newUser.setUserId(generateUniqueUserId()); // 生成唯一用户ID
         newUser.setUserName(username); // 确保设置了用户名
        newUser.setEmail(email);
        newUser.setRegisterIp(ip);
        newUser.setLoginIp(ip);
        newUser.setEncryptedPassword(password); // 加密密码
        newUser.setLoginStatus(false);
        newUser.setAvatarUrl(""); // 默认头像为空
        newUser.setLastReadTs(System.currentTimeMillis());
        newUser.setCreatedAt(new Date());
        newUser.setUpdatedAt(new Date());
        
        int result = userMapper.insert(newUser);
        
        // 创建默认用户设置
        UserSettings defaultSettings = new UserSettings();
        defaultSettings.setUserId(newUser.getUserId());
        defaultSettings.setDarkMode(false);
        defaultSettings.setBackgroundImage(null);
        defaultSettings.setBackgroundOpacity(100);
        defaultSettings.setLayout("default");
        defaultSettings.setPersonalSignature("");
        defaultSettings.setGender("");
        defaultSettings.setAge(null);
        defaultSettings.setCreatedAt(new Date());
        defaultSettings.setUpdatedAt(new Date());
        userSettingsMapper.insert(defaultSettings);
        
        return result > 0;
    }
    
    @Override
    public Boolean logout(String userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setLoginStatus(false);
            userMapper.updateById(user);
            // 从Redis中移除token
            JwtTokenUtil.removeTokenFromRedis(userId);
            return true;
        }
        return false;
    }
    
    @Override
    public UserProfileVO getUserInfo(String userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            UserProfileVO userProfileVO = UserProfileVO.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .Ip(user.getLoginIp())
                    .loginStatus(user.getLoginStatus()) // 添加用户在线状态
                    .build();
            
            // 如果头像URL是相对路径，转换为完整URL
            if (userProfileVO.getAvatarUrl() != null && !userProfileVO.getAvatarUrl().startsWith("http")) {
                userProfileVO.setAvatarUrl(ossProperties.getOssDomainPrefix() + userProfileVO.getAvatarUrl());
            }
            
            // 获取个人签名
            QueryWrapper<UserSettings> settingsWrapper = new QueryWrapper<>();
            settingsWrapper.eq("user_id", userId);
            UserSettings userSettings = userSettingsMapper.selectOne(settingsWrapper);
            if (userSettings != null) {
                userProfileVO.setPersonalSignature(userSettings.getPersonalSignature());
            }
            
            return userProfileVO;
        }
        return null;
    }
    
    @Override
    public Boolean updateAvatar(String userId, String newAvatarUrl) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            // 删除旧头像文件（如果存在且不是默认头像）
            String oldAvatarUrl = user.getAvatarUrl();
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                try {
                    // 如果是OSS文件，调用OSS删除方法
                    if (oldAvatarUrl.contains("oss") || oldAvatarUrl.contains("aliyuncs.com") || 
                        (oldAvatarUrl.startsWith("avatars/") || oldAvatarUrl.startsWith("backgrounds/") || oldAvatarUrl.startsWith("uploads/"))) {
                        ossUtil.deleteFileFromOSS(oldAvatarUrl);
                    }
                } catch (Exception e) {
                    System.out.println("删除旧头像文件失败: " + e.getMessage());
                }
            }
            
            // 如果新头像URL是完整URL，提取相对路径存储到数据库
            String avatarUrlToSave = newAvatarUrl;
            if (newAvatarUrl != null && newAvatarUrl.startsWith("http") && newAvatarUrl.contains(ossProperties.getBucketName())) {
                avatarUrlToSave = ossUtil.extractObjectNameFromUrl(newAvatarUrl);
            }
            
            // 更新用户头像URL
            user.setAvatarUrl(avatarUrlToSave);
            user.setUpdatedAt(new Date());
            userMapper.updateById(user);
            return true;
        }
        return false;
    }
    
    @Override
    public UserSettingsVO getUserSettings(String userId) {
        QueryWrapper<UserSettings> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        UserSettings userSettings = userSettingsMapper.selectOne(wrapper);
        
        if (userSettings != null) {
            UserSettingsVO userSettingsVO = UserSettingsVO.builder()
                    .userId(userId)
                    .darkMode(userSettings.getDarkMode())
                    .backgroundImage(userSettings.getBackgroundImage())
                    .backgroundOpacity(userSettings.getBackgroundOpacity())
                    .layout(userSettings.getLayout())
                    .personalSignature(userSettings.getPersonalSignature())
                    .gender(userSettings.getGender())
                    .age(userSettings.getAge())
                    .build();
            
            // 如果背景图片URL是相对路径，转换为完整URL
            if (userSettingsVO.getBackgroundImage() != null && 
                !userSettingsVO.getBackgroundImage().isEmpty() && 
                !userSettingsVO.getBackgroundImage().startsWith("http")) {
                userSettingsVO.setBackgroundImage(ossProperties.getOssDomainPrefix() + userSettingsVO.getBackgroundImage());
            }
            
            return userSettingsVO;
        }
        
        // 如果没有找到设置，返回默认设置
        return UserSettingsVO.builder()
                .userId(userId)
                .darkMode(false)
                .backgroundImage(null)
                .backgroundOpacity(100)
                .layout("default")
                .personalSignature("")
                .gender("")
                .age(null)
                .build();
    }
    
    @Override
    public Boolean saveUserSettings(UserSettingsVO userSettingsVO) {
        QueryWrapper<UserSettings> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userSettingsVO.getUserId());
        UserSettings existingSettings = userSettingsMapper.selectOne(wrapper);
        
        UserSettings userSettings = new UserSettings();
        BeanUtils.copyProperties(userSettingsVO, userSettings);
        
        // 如果背景图片URL是完整URL，提取相对路径存储到数据库
        if (userSettings.getBackgroundImage() != null && userSettings.getBackgroundImage().startsWith("http") && userSettings.getBackgroundImage().contains(ossProperties.getBucketName())) {
            userSettings.setBackgroundImage(ossUtil.extractObjectNameFromUrl(userSettings.getBackgroundImage()));
        }
        
        // 添加个人签名长度限制
        if (userSettingsVO.getPersonalSignature() != null && userSettingsVO.getPersonalSignature().length() > 200) {
            userSettings.setPersonalSignature(userSettingsVO.getPersonalSignature().substring(0, 200));
        }
        
        userSettings.setUpdatedAt(new Date());
        
        if (existingSettings != null) {
            // 检查背景图片是否发生变化，如果变化则删除旧的背景图片
            if (existingSettings.getBackgroundImage() != null && 
                userSettingsVO.getBackgroundImage() != null &&
                !existingSettings.getBackgroundImage().isEmpty() &&
                !userSettingsVO.getBackgroundImage().isEmpty()) {
                
                // 确保比较的是相同格式的URL（都转换为相对路径再比较）
                String existingImagePath = existingSettings.getBackgroundImage();
                String newImagePath = userSettingsVO.getBackgroundImage();
                
                // 如果现有图片是完整URL，提取相对路径
                if (existingImagePath.startsWith("http")) {
                    existingImagePath = ossUtil.extractObjectNameFromUrl(existingImagePath);
                }
                
                // 如果新图片是完整URL，提取相对路径
                if (newImagePath.startsWith("http") && newImagePath.contains(ossProperties.getBucketName())) {
                    newImagePath = ossUtil.extractObjectNameFromUrl(newImagePath);
                }
                
                // 只有在图片路径真正发生变化时才删除旧图片
                if (!existingImagePath.equals(newImagePath)) {
                    try {
                        String oldBackgroundImage = existingSettings.getBackgroundImage();
                        // 删除OSS文件
                        if (oldBackgroundImage.contains("oss") || oldBackgroundImage.contains("aliyuncs.com") || 
                            (oldBackgroundImage.startsWith("avatars/") || oldBackgroundImage.startsWith("backgrounds/") || oldBackgroundImage.startsWith("uploads/"))) {
                            // 实际删除OSS文件
                            boolean deleted = ossUtil.deleteFileFromOSS(oldBackgroundImage);
                            if (deleted) {
                                System.out.println("已删除用户的旧OSS背景图片: " + oldBackgroundImage);
                            } else {
                                System.out.println("删除用户的旧OSS背景图片失败: " + oldBackgroundImage);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("删除用户的旧背景图片失败: " + existingSettings.getBackgroundImage() + ", 错误: " + e.getMessage());
                    }
                }
            }
            
            // 如果背景图片URL是完整URL，提取相对路径存储到数据库
            if (userSettings.getBackgroundImage() != null && userSettings.getBackgroundImage().startsWith("http") && userSettings.getBackgroundImage().contains(ossProperties.getBucketName())) {
                userSettings.setBackgroundImage(ossUtil.extractObjectNameFromUrl(userSettings.getBackgroundImage()));
            }
            
            return userSettingsMapper.update(userSettings, wrapper) > 0;
        } else {
            userSettings.setCreatedAt(new Date());
            return userSettingsMapper.insert(userSettings) > 0;
        }
    }
    
    @Override
    public List<UserBasicVO> searchUsers(String keyword) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.like("user_id", keyword).or().like("user_name", keyword));
        queryWrapper.last("LIMIT 20"); // 限制返回结果数量
        
        List<User> users = userMapper.selectList(queryWrapper);
        List<UserBasicVO> userBasicVOs = new ArrayList<>();
        
        for (User user : users) {
            UserBasicVO userBasicVO = UserBasicVO.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .Ip(user.getLoginIp())
                    .loginStatus(user.getLoginStatus())
                    .build();
            
            // 如果头像URL是相对路径，转换为完整URL
            if (userBasicVO.getAvatarUrl() != null && !userBasicVO.getAvatarUrl().startsWith("http")) {
                userBasicVO.setAvatarUrl(ossProperties.getOssDomainPrefix() + userBasicVO.getAvatarUrl());
            }
            
            userBasicVOs.add(userBasicVO);
        }
        
        return userBasicVOs;
    }
    
    @Override
    public List<UserBasicVO> searchUsers(String keyword, String userId) {
        // 先查询用户屏蔽了哪些用户
        QueryWrapper<Friend> blockWrapper = new QueryWrapper<>();
        blockWrapper.eq("user_id", userId).eq("status", "BLOCKED");
        List<Friend> blockedFriends = friendMapper.selectList(blockWrapper);
        
        // 提取被屏蔽的用户ID列表
        List<String> blockedUserIds = new ArrayList<>();
        for (Friend friend : blockedFriends) {
            blockedUserIds.add(friend.getFriendId());
        }
        
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.like("user_id", keyword).or().like("user_name", keyword));
        
        // 排除被屏蔽的用户
        if (!blockedUserIds.isEmpty()) {
            queryWrapper.notIn("user_id", blockedUserIds);
        }
        
        // 排除自己
        queryWrapper.ne("user_id", userId);
        
        queryWrapper.last("LIMIT 20"); // 限制返回结果数量
        
        List<User> users = userMapper.selectList(queryWrapper);
        List<UserBasicVO> userBasicVOs = new ArrayList<>();
        
        for (User user : users) {
            UserBasicVO userBasicVO = UserBasicVO.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .Ip(user.getLoginIp())
                    .loginStatus(user.getLoginStatus())
                    .build();
            
            // 如果头像URL是相对路径，转换为完整URL
            if (userBasicVO.getAvatarUrl() != null && !userBasicVO.getAvatarUrl().startsWith("http")) {
                userBasicVO.setAvatarUrl(ossProperties.getOssDomainPrefix() + userBasicVO.getAvatarUrl());
            }
            
            userBasicVOs.add(userBasicVO);
        }
        
        return userBasicVOs;
    }
    @Override
    public boolean updateUserStatus(String userId, boolean isOnline) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setLoginStatus(isOnline);
            userMapper.updateById(user);
            return true;
        }
        return false;
    }
}