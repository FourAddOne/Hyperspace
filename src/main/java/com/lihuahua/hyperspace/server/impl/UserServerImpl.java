package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.mapper.UserSettingsMapper;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.entity.UserSettings;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.models.vo.UserSettingsVO;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import com.lihuahua.hyperspace.utils.LocalFileUtil;
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
    
    @Override
    public UserLoginVO login(Map<String, String> credential) {
        System.out.println("开始登录验证，凭据: " + credential);
        
        String userId = credential.get("userId");
        String email = credential.get("email");
        String password = credential.get("password");
        String ip = credential.get("ip");
        
        System.out.println("提取的登录信息 - userId: " + userId + ", email: " + email + ", password: " + password + ", ip: " + ip);
        
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
        System.out.println("开始密码验证 - 数据库中的密码: " + user.getPassword() + ", 输入的密码: " + password);
        if (!user.login(credential)) {
            System.out.println("错误：密码验证失败");
            throw new RuntimeException("密码错误");
        }
        
        System.out.println("密码验证成功");
        
        // 更新登录信息
        user.setLoginIp(ip);
        user.setLoginStatus(true); // 设置用户为在线状态
        userMapper.updateById(user);
        System.out.println("更新用户登录信息完成");
        
        // 生成访问令牌和刷新令牌
        String accessToken = JwtTokenUtil.generateAccessToken(user.getUserId());
        String refreshToken = JwtTokenUtil.generateRefreshToken(user.getUserId());
        System.out.println("生成的访问令牌: " + accessToken);
        System.out.println("生成的刷新令牌: " + refreshToken);
        
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
        
        System.out.println("构建的用户登录VO: " + userLoginVO);
        return userLoginVO;
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
        String userId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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
            return UserLoginVO.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .Ip(user.getLoginIp())
                    .loginStatus(user.getLoginStatus()) // 添加用户在线状态
                    .build();
        }
        return null;
    }
    
    @Override
    public Boolean updateAvatar(String userId, String newAvatarUrl) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            // 删除旧头像文件（如果存在且不是默认头像）
            String oldAvatarUrl = user.getAvatarUrl();
            if (oldAvatarUrl != null && oldAvatarUrl.contains("/uploads/")) {
                try {
                    localFileUtil.deleteLocalFile(oldAvatarUrl);
                    System.out.println("已删除用户 " + userId + " 的旧头像: " + oldAvatarUrl);
                } catch (Exception e) {
                    System.out.println("删除用户 " + userId + " 的旧头像失败: " + oldAvatarUrl + ", 错误: " + e.getMessage());
                }
            }
            
            // 更新用户头像URL
            user.setAvatarUrl(newAvatarUrl);
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
                    localFileUtil.deleteLocalFile(existingSettings.getBackgroundImage());
                    System.out.println("已删除用户的旧背景图片: " + existingSettings.getBackgroundImage());
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
        queryWrapper.like("user_name", keyword).or().like("email", keyword);
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
            userLoginVOs.add(userLoginVO);
        }
        
        return userLoginVOs;
    }
    
    @Override
    public List<UserLoginVO> searchUsers(String keyword, String excludeUserId) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("user_name", keyword).or().like("email", keyword);
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