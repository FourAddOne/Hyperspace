package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.mapper.UserSettingsMapper;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.entity.UserSettings;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.models.vo.UserSettingsVO;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.IdUtil;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import com.lihuahua.hyperspace.utils.LocalFileUtil;
import com.lihuahua.hyperspace.utils.OssProperties;
import com.lihuahua.hyperspace.utils.OssUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServerImpl implements UserServer {
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private UserSettingsMapper userSettingsMapper;
    
    @Resource
    private LocalFileUtil localFileUtil;
    
    @Resource
    private OssUtil ossUtil;
    
    @Resource
    private OssProperties ossProperties;
    
    @Override
    public UserLoginVO login(Map<String, String> credential) {
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
        
        // 构建返回的用户信息
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .Ip(user.getLoginIp())
                .loginStatus(user.getLoginStatus()) // 添加用户在线状态
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();

        return userLoginVO;
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
        String userId = generateUniqueUserId(); // 使用新的唯一ID生成方法
        newUser.setUserId(userId);
        newUser.setUserName(username);
        newUser.setEmail(email);
        // 使用加密方法设置密码
        newUser.setEncryptedPassword(password);
        newUser.setRegisterIp(ip);
        newUser.setLoginIp(ip);
        newUser.setLoginStatus(false);
        newUser.setCreatedAt(new Date());
        newUser.setUpdatedAt(new Date());
        
        return userMapper.insert(newUser) > 0;
    }
    
    @Override
    public Boolean logout(String userId) {
        // 更新用户登录状态
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setLoginStatus(false); // 设置用户为离线状态
            userMapper.updateById(user);
            
            // 从Redis中删除用户的token
            JwtTokenUtil.removeTokenFromRedis(userId);
            return true;
        }
        return false;
    }
    
    @Override
    public UserLoginVO getUserInfo(String userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            UserLoginVO userLoginVO = UserLoginVO.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .Ip(user.getLoginIp())
                    .loginStatus(user.getLoginStatus()) // 添加用户在线状态
                    .build();
            
            // 如果头像URL是相对路径，转换为完整URL
            if (userLoginVO.getAvatarUrl() != null && !userLoginVO.getAvatarUrl().startsWith("http")) {
                userLoginVO.setAvatarUrl(ossProperties.getOssDomainPrefix() + userLoginVO.getAvatarUrl());
            }
            
            return userLoginVO;
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
                    // 如果是本地文件，使用本地删除方法
                    if (oldAvatarUrl.startsWith("/uploads/")) {
                        localFileUtil.deleteLocalFile(oldAvatarUrl);
                        System.out.println("已删除用户 " + userId + " 的旧头像: " + oldAvatarUrl);
                    } 
                    // 如果是OSS文件，调用OSS删除方法
                    else if (oldAvatarUrl.contains("oss") || oldAvatarUrl.contains("aliyuncs.com")) {
                        // 实际删除OSS文件
                        boolean deleted = ossUtil.deleteFileFromOSS(oldAvatarUrl);
                        if (deleted) {
                            System.out.println("已删除用户 " + userId + " 的旧OSS头像: " + oldAvatarUrl);
                        } else {
                            System.out.println("删除用户 " + userId + " 的旧OSS头像失败: " + oldAvatarUrl);
                        }
                    }
                    // 如果是相对路径，说明是OSS文件
                    else if (!oldAvatarUrl.startsWith("http")) {
                        // 实际删除OSS文件
                        boolean deleted = ossUtil.deleteFileFromOSS(oldAvatarUrl);
                        if (deleted) {
                            System.out.println("已删除用户 " + userId + " 的旧OSS头像: " + oldAvatarUrl);
                        } else {
                            System.out.println("删除用户 " + userId + " 的旧OSS头像失败: " + oldAvatarUrl);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("删除用户 " + userId + " 的旧头像失败: " + oldAvatarUrl + ", 错误: " + e.getMessage());
                }
            }
            
            // 如果新头像URL是完整URL，提取相对路径存储到数据库
            String avatarPathToSave = newAvatarUrl;
            if (newAvatarUrl != null && newAvatarUrl.startsWith("http") && newAvatarUrl.contains(ossProperties.getBucketName())) {
                avatarPathToSave = ossUtil.extractObjectNameFromUrl(newAvatarUrl);
            }
            
            // 更新用户头像URL
            user.setAvatarUrl(avatarPathToSave);
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
            UserSettingsVO userSettingsVO = new UserSettingsVO();
            BeanUtils.copyProperties(userSettings, userSettingsVO);
            
            // 如果背景图片URL是相对路径，转换为完整URL
            if (userSettingsVO.getBackgroundImage() != null && !userSettingsVO.getBackgroundImage().startsWith("http")) {
                userSettingsVO.setBackgroundImage(ossProperties.getOssDomainPrefix() + userSettingsVO.getBackgroundImage());
            }
            
            return userSettingsVO;
        }
        
        // 如果用户设置不存在，返回默认设置
        return UserSettingsVO.builder()
                .userId(userId)
                .darkMode(false)
                .backgroundImage("")
                .backgroundOpacity(100) // 默认透明度为100%
                .layout("default")
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
                !existingSettings.getBackgroundImage().equals(userSettingsVO.getBackgroundImage())) {
                try {
                    String oldBackgroundImage = existingSettings.getBackgroundImage();
                    // 如果是本地文件，使用本地删除方法
                    if (oldBackgroundImage.startsWith("/uploads/")) {
                        localFileUtil.deleteLocalFile(oldBackgroundImage);
                        System.out.println("已删除用户的旧背景图片: " + oldBackgroundImage);
                    }
                    // 如果是OSS文件，调用OSS删除方法
                    else if (oldBackgroundImage.contains("oss") || oldBackgroundImage.contains("aliyuncs.com")) {
                        // 实际删除OSS文件
                        boolean deleted = ossUtil.deleteFileFromOSS(oldBackgroundImage);
                        if (deleted) {
                            System.out.println("已删除用户的旧OSS背景图片: " + oldBackgroundImage);
                        } else {
                            System.out.println("删除用户的旧OSS背景图片失败: " + oldBackgroundImage);
                        }
                    }
                    // 如果是相对路径，说明是OSS文件
                    else if (!oldBackgroundImage.startsWith("http")) {
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
            
            return userSettingsMapper.update(userSettings, wrapper) > 0;
        } else {
            userSettings.setCreatedAt(new Date());
            return userSettingsMapper.insert(userSettings) > 0;
        }
    }
    
    @Override
    public List<UserLoginVO> searchUsers(String keyword) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.like("user_id", keyword).or().like("user_name", keyword));
        queryWrapper.last("LIMIT 20"); // 限制返回结果数量
        
        List<User> users = userMapper.selectList(queryWrapper);
        List<UserLoginVO> userLoginVOs = new ArrayList<>();
        
        for (User user : users) {
            UserLoginVO userLoginVO = UserLoginVO.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .Ip(user.getLoginIp())
                    .build();
            
            // 如果头像URL是相对路径，转换为完整URL
            if (userLoginVO.getAvatarUrl() != null && !userLoginVO.getAvatarUrl().startsWith("http")) {
                userLoginVO.setAvatarUrl(ossProperties.getOssDomainPrefix() + userLoginVO.getAvatarUrl());
            }
            
            userLoginVOs.add(userLoginVO);
        }
        
        return userLoginVOs;
    }
    
    @Override
    public List<UserLoginVO> searchUsers(String keyword, String excludeUserId) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.like("user_id", keyword).or().like("user_name", keyword));
        // 排除指定用户
        if (excludeUserId != null && !excludeUserId.isEmpty()) {
            queryWrapper.ne("user_id", excludeUserId);
        }
        queryWrapper.last("LIMIT 20"); // 限制返回结果数量
        
        List<User> users = userMapper.selectList(queryWrapper);
        List<UserLoginVO> userLoginVOs = new ArrayList<>();
        
        for (User user : users) {
            UserLoginVO userLoginVO = UserLoginVO.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .Ip(user.getLoginIp())
                    .build();
            
            // 如果头像URL是相对路径，转换为完整URL
            if (userLoginVO.getAvatarUrl() != null && !userLoginVO.getAvatarUrl().startsWith("http")) {
                userLoginVO.setAvatarUrl(ossProperties.getOssDomainPrefix() + userLoginVO.getAvatarUrl());
            }
            
            userLoginVOs.add(userLoginVO);
        }
        
        return userLoginVOs;
    }
    
    @Override
    public boolean isFriend(String userId, String friendId) {
        // 这里应该调用FriendService来检查是否为好友
        // 为了简化，暂时返回false
        return false;
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