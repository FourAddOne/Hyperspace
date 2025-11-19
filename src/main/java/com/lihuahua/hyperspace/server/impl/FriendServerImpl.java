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
        
        // 创建好友请求记录
        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus("pending");
        friend.setCreatedAt(new Date());
        friend.setUpdatedAt(new Date());
        friendMapper.insert(friend);
        
        return true;
    }
    
    @Override
    public boolean acceptFriendRequest(String userId, String requesterId) {
        // 更新好友请求状态
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", requesterId).eq("friend_id", userId).eq("status", "pending");
        Friend friendRequest = friendMapper.selectOne(wrapper);
        
        if (friendRequest == null) {
            throw new RuntimeException("未找到好友请求");
        }
        
        friendRequest.setStatus("accepted");
        friendRequest.setUpdatedAt(new Date());
        friendMapper.update(friendRequest, wrapper);
        
        // 创建反向好友关系（双向好友）
        Friend reverseFriend = new Friend();
        reverseFriend.setUserId(userId);
        reverseFriend.setFriendId(requesterId);
        reverseFriend.setStatus("accepted");
        reverseFriend.setCreatedAt(new Date());
        reverseFriend.setUpdatedAt(new Date());
        friendMapper.insert(reverseFriend);
        
        return true;
    }
    
    @Override
    public boolean rejectFriendRequest(String userId, String requesterId) {
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", requesterId).eq("friend_id", userId).eq("status", "pending");
        Friend friendRequest = friendMapper.selectOne(wrapper);
        
        if (friendRequest == null) {
            throw new RuntimeException("未找到好友请求");
        }
        
        friendMapper.delete(wrapper);
        return true;
    }
    
    @Override
    public List<UserLoginVO> getFriendList(String userId) {
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("status", "accepted");
        List<Friend> friends = friendMapper.selectList(wrapper);
        
        List<UserLoginVO> friendList = new ArrayList<>();
        for (Friend friend : friends) {
            User user = userMapper.selectById(friend.getFriendId());
            if (user != null) {
                UserLoginVO userLoginVO = UserLoginVO.builder()
                        .userId(user.getUserId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .Ip(user.getLoginIp())
                        .loginStatus(user.getLoginStatus()) // 添加用户在线状态
                        .build();
                
                // 获取好友的个人签名
                QueryWrapper<UserSettings> settingsWrapper = new QueryWrapper<>();
                settingsWrapper.eq("user_id", friend.getFriendId());
                UserSettings userSettings = userSettingsMapper.selectOne(settingsWrapper);
                if (userSettings != null && userSettings.getPersonalSignature() != null && !userSettings.getPersonalSignature().isEmpty()) {
                    // 限制签名长度为30个字符，超过则截断并添加省略号
                    String signature = userSettings.getPersonalSignature();
                    if (signature.length() > 30) {
                        signature = signature.substring(0, 30) + "...";
                    }
                    userLoginVO.setPersonalSignature(signature);
                }
                
                friendList.add(userLoginVO);
            }
        }
        
        return friendList;
    }
    
    @Override
    public List<UserLoginVO> getFriendRequests(String userId) {
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.eq("friend_id", userId).eq("status", "pending");
        List<Friend> requests = friendMapper.selectList(wrapper);
        
        List<UserLoginVO> requestList = new ArrayList<>();
        for (Friend request : requests) {
            User user = userMapper.selectById(request.getUserId());
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
        // 删除双向好友关系
        QueryWrapper<Friend> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("user_id", userId).eq("friend_id", friendId);
        friendMapper.delete(wrapper1);
        
        QueryWrapper<Friend> wrapper2 = new QueryWrapper<>();
        wrapper2.eq("user_id", friendId).eq("friend_id", userId);
        friendMapper.delete(wrapper2);
        
        return true;
    }
    
    @Override
    public boolean isFriend(String userId, String friendId) {
        return friendMapper.isFriend(userId, friendId);
    }
}