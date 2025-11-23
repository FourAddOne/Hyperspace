package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.mapper.FriendMapper;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.mapper.UserSettingsMapper;
import com.lihuahua.hyperspace.models.entity.Friend;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.entity.UserSettings;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.server.FriendServer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FriendServerImpl implements FriendServer {
    
    @Resource
    private FriendMapper friendMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private UserSettingsMapper userSettingsMapper;
    
    @Override
    public boolean sendFriendRequest(String userId, String friendId) {
        // 检查是否已经是好友
        if (friendMapper.isFriend(userId, friendId)) {
            throw new RuntimeException("你们已经是好友了");
        }
        
        // 检查是否已经发送过请求
        if (friendMapper.hasPendingRequest(userId, friendId)) {
            throw new RuntimeException("好友请求已发送，请等待对方接受");
        }
        
        // 创建好友请求记录，确保较小的ID存储在userOne字段
        Friend friend = new Friend();
        if (userId.compareTo(friendId) < 0) {
            friend.setUserOne(userId);
            friend.setUserSec(friendId);
        } else {
            friend.setUserOne(friendId);
            friend.setUserSec(userId);
        }
        friend.setStatus("PENDING");
        friend.setBlockStatus("NONE");
        friend.setUserOneRemark("");
        friend.setUserSecRemark("");
        long currentTime = System.currentTimeMillis();
        friend.setCreatedAt(currentTime);
        friend.setUpdatedAt(currentTime);
        friendMapper.insert(friend);
        
        return true;
    }
    
    @Override
    public boolean acceptFriendRequest(String userId, String requesterId) {
        // 更新好友请求状态
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        // 确保正确的用户ID顺序
        if (requesterId.compareTo(userId) < 0) {
            wrapper.eq("user_one", requesterId).eq("user_sec", userId);
        } else {
            wrapper.eq("user_one", userId).eq("user_sec", requesterId);
        }
        wrapper.eq("status", "PENDING");
        
        Friend friendRequest = friendMapper.selectOne(wrapper);
        
        if (friendRequest == null) {
            throw new RuntimeException("未找到好友请求");
        }
        
        friendRequest.setStatus("ACCEPTED");
        friendRequest.setUpdatedAt(System.currentTimeMillis());
        friendMapper.update(friendRequest, wrapper);
        
        return true;
    }
    
    @Override
    public boolean rejectFriendRequest(String userId, String requesterId) {
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        // 确保正确的用户ID顺序
        if (requesterId.compareTo(userId) < 0) {
            wrapper.eq("user_one", requesterId).eq("user_sec", userId);
        } else {
            wrapper.eq("user_one", userId).eq("user_sec", requesterId);
        }
        wrapper.eq("status", "PENDING");
        
        Friend friendRequest = friendMapper.selectOne(wrapper);
        
        if (friendRequest == null) {
            throw new RuntimeException("未找到好友请求");
        }
        
        friendMapper.delete(wrapper);
        return true;
    }
    
    @Override
    public List<UserLoginVO> getFriendList(String userId) {
        // 查询用户作为userOne的所有好友关系
        QueryWrapper<Friend> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("user_one", userId).eq("status", "ACCEPTED");
        List<Friend> friendsAsUserOne = friendMapper.selectList(wrapper1);
        
        // 查询用户作为userSec的所有好友关系
        QueryWrapper<Friend> wrapper2 = new QueryWrapper<>();
        wrapper2.eq("user_sec", userId).eq("status", "ACCEPTED");
        List<Friend> friendsAsUserSec = friendMapper.selectList(wrapper2);
        
        List<UserLoginVO> friendList = new ArrayList<>();
        
        // 处理用户作为userOne的好友关系
        for (Friend friend : friendsAsUserOne) {
            User user = userMapper.selectById(friend.getUserSec());
            if (user != null) {
                UserLoginVO userLoginVO = UserLoginVO.builder()
                        .userId(user.getUserId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .Ip(user.getLoginIp())
                        .loginStatus(user.getLoginStatus())
                        .build();
                
                // 获取好友的个人签名
                QueryWrapper<UserSettings> settingsWrapper = new QueryWrapper<>();
                settingsWrapper.eq("user_id", friend.getUserSec());
                UserSettings userSettings = userSettingsMapper.selectOne(settingsWrapper);
                if (userSettings != null && userSettings.getPersonalSignature() != null && !userSettings.getPersonalSignature().isEmpty()) {
                    // 限制签名长度为30个字符，超过则截断并添加省略号
                    String signature = userSettings.getPersonalSignature();
                    if (signature.length() > 30) {
                        signature = signature.substring(0, 30) + "...";
                    }
                    userLoginVO.setPersonalSignature(signature);
                }
                
                // 添加备注名（如果存在）
                if (friend.getUserOneRemark() != null && !friend.getUserOneRemark().isEmpty()) {
                    userLoginVO.setUserName(friend.getUserOneRemark());
                }
                
                friendList.add(userLoginVO);
            }
        }
        
        // 处理用户作为userSec的好友关系
        for (Friend friend : friendsAsUserSec) {
            User user = userMapper.selectById(friend.getUserOne());
            if (user != null) {
                UserLoginVO userLoginVO = UserLoginVO.builder()
                        .userId(user.getUserId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .Ip(user.getLoginIp())
                        .loginStatus(user.getLoginStatus())
                        .build();
                
                // 获取好友的个人签名
                QueryWrapper<UserSettings> settingsWrapper = new QueryWrapper<>();
                settingsWrapper.eq("user_id", friend.getUserOne());
                UserSettings userSettings = userSettingsMapper.selectOne(settingsWrapper);
                if (userSettings != null && userSettings.getPersonalSignature() != null && !userSettings.getPersonalSignature().isEmpty()) {
                    // 限制签名长度为30个字符，超过则截断并添加省略号
                    String signature = userSettings.getPersonalSignature();
                    if (signature.length() > 30) {
                        signature = signature.substring(0, 30) + "...";
                    }
                    userLoginVO.setPersonalSignature(signature);
                }
                
                // 添加备注名（如果存在）
                if (friend.getUserSecRemark() != null && !friend.getUserSecRemark().isEmpty()) {
                    userLoginVO.setUserName(friend.getUserSecRemark());
                }
                
                friendList.add(userLoginVO);
            }
        }
        
        return friendList;
    }
    
    @Override
    public List<UserLoginVO> getFriendRequests(String userId) {
        // 查询用户作为userSec的所有待处理好友请求（即别人发送给用户的好友请求）
        QueryWrapper<Friend> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("user_sec", userId).eq("status", "PENDING");
        List<Friend> requests1 = friendMapper.selectList(wrapper1);
        
        // 查询用户作为userOne的所有待处理好友请求（即别人发送给用户的好友请求）
        QueryWrapper<Friend> wrapper2 = new QueryWrapper<>();
        wrapper2.eq("user_one", userId).eq("status", "PENDING");
        List<Friend> requests2 = friendMapper.selectList(wrapper2);
        
        List<UserLoginVO> requestList = new ArrayList<>();
        
        // 处理用户作为userSec的好友请求（userId > requesterId的情况）
        for (Friend request : requests1) {
            User user = userMapper.selectById(request.getUserOne());
            if (user != null) {
                UserLoginVO userLoginVO = UserLoginVO.builder()
                        .userId(user.getUserId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .Ip(user.getLoginIp())
                        .build();
                requestList.add(userLoginVO);
            }
        }
        
        // 处理用户作为userOne的好友请求（userId < requesterId的情况）
        for (Friend request : requests2) {
            User user = userMapper.selectById(request.getUserSec());
            if (user != null) {
                UserLoginVO userLoginVO = UserLoginVO.builder()
                        .userId(user.getUserId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .Ip(user.getLoginIp())
                        .build();
                requestList.add(userLoginVO);
            }
        }
        
        return requestList;
    }
    
    @Override
    public boolean deleteFriend(String userId, String friendId) {
        // 删除好友关系
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        if (userId.compareTo(friendId) < 0) {
            wrapper.eq("user_one", userId).eq("user_sec", friendId);
        } else {
            wrapper.eq("user_one", friendId).eq("user_sec", userId);
        }
        friendMapper.delete(wrapper);
        
        return true;
    }
    
    @Override
    public boolean isFriend(String userId, String friendId) {
        return friendMapper.isFriend(userId, friendId);
    }
    
    // 添加备注功能
    public boolean updateFriendRemark(String userId, String friendId, String remark) {
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        Friend friend;
        
        if (userId.compareTo(friendId) < 0) {
            // userId是userOne
            wrapper.eq("user_one", userId).eq("user_sec", friendId);
            friend = friendMapper.selectOne(wrapper);
            if (friend != null) {
                friend.setUserOneRemark(remark);
            }
        } else {
            // userId是userSec
            wrapper.eq("user_one", friendId).eq("user_sec", userId);
            friend = friendMapper.selectOne(wrapper);
            if (friend != null) {
                friend.setUserSecRemark(remark);
            }
        }
        
        if (friend != null) {
            friend.setUpdatedAt(System.currentTimeMillis());
            friendMapper.update(friend, wrapper);
            return true;
        }
        
        return false;
    }
}