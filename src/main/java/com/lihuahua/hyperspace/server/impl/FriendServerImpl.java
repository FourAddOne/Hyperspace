package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.mapper.FriendMapper;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.mapper.UserSettingsMapper;
import com.lihuahua.hyperspace.models.entity.Friend;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.entity.UserSettings;
import com.lihuahua.hyperspace.models.vo.FriendVO;
import com.lihuahua.hyperspace.models.vo.UserBasicVO;
import com.lihuahua.hyperspace.server.FriendServer;
import com.lihuahua.hyperspace.utils.OssProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FriendServerImpl implements FriendServer {
    
    @Resource
    private FriendMapper friendMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private UserSettingsMapper userSettingsMapper;
    
    @Resource
    private OssProperties ossProperties;
    
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
        
        // 检查是否被对方屏蔽
        QueryWrapper<Friend> blockWrapper = new QueryWrapper<>();
        blockWrapper.eq("user_id", friendId).eq("friend_id", userId).eq("status", "BLOCKED");
        if (friendMapper.selectCount(blockWrapper) > 0) {
            throw new RuntimeException("无法发送好友请求");
        }
        
        // 创建好友请求记录 (用户向好友发送请求)
        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus("PENDING");
        friend.setBlockStatus("NONE");
        friend.setRemark("");
        long currentTime = System.currentTimeMillis();
        friend.setCreatedAt(currentTime);
        friend.setUpdatedAt(currentTime);
        System.out.println(friend);
        friendMapper.insert(friend);
        return true;
    }
    
    @Override
    public boolean acceptFriendRequest(String userId, String requesterId) {
        // 更新好友请求状态 (用户接受来自请求者的请求)
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", requesterId).eq("friend_id", userId).eq("status", "PENDING");
        Friend friendRequest = friendMapper.selectOne(wrapper);
        
        if (friendRequest == null) {
            throw new RuntimeException("未找到好友请求");
        }
        
        // 更新请求状态为已接受
        friendRequest.setStatus("ACCEPTED");
        friendRequest.setUpdatedAt(System.currentTimeMillis());
        friendMapper.update(friendRequest, wrapper);
        
        // 创建反向好友关系（好友到用户的关系）
        Friend reverseFriend = new Friend();
        reverseFriend.setUserId(userId);
        reverseFriend.setFriendId(requesterId);
        reverseFriend.setStatus("ACCEPTED");
        reverseFriend.setBlockStatus("NONE");
        reverseFriend.setRemark("");
        long currentTime = System.currentTimeMillis();
        reverseFriend.setCreatedAt(currentTime);
        reverseFriend.setUpdatedAt(currentTime);
        friendMapper.insert(reverseFriend);
        
        return true;
    }
    
    @Override
    public boolean rejectFriendRequest(String userId, String requesterId) {
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", requesterId).eq("friend_id", userId).eq("status", "PENDING");
        Friend friendRequest = friendMapper.selectOne(wrapper);
        
        if (friendRequest == null) {
            throw new RuntimeException("未找到好友请求");
        }
        
        friendMapper.delete(wrapper);
        return true;
    }
    
    @Override
    public boolean blockFriendRequest(String userId, String requesterId) {
        // 先拒绝好友请求
        rejectFriendRequest(userId, requesterId);
        
        // 然后创建一个屏蔽记录，防止该用户再次发送请求
        Friend blockRecord = new Friend();
        blockRecord.setUserId(userId); // 当前用户屏蔽了requesterId
        blockRecord.setFriendId(requesterId);
        blockRecord.setStatus("BLOCKED");
        blockRecord.setBlockStatus("BLOCKED"); // 简化屏蔽状态
        blockRecord.setRemark("");
        long currentTime = System.currentTimeMillis();
        blockRecord.setCreatedAt(currentTime);
        blockRecord.setUpdatedAt(currentTime);
        friendMapper.insert(blockRecord);
        
        return true;
    }
    
    @Override
    public List<FriendVO> getFriendList(String userId) {
        // 查询用户的所有好友关系 (用户作为发起方)
        QueryWrapper<Friend> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("user_id", userId).eq("status", "ACCEPTED");
        List<Friend> friendsAsUser = friendMapper.selectList(wrapper1);

        List<FriendVO> friendList = new ArrayList<>();
        
        // 处理用户的好友关系
        for (Friend friend : friendsAsUser) {
            User user = userMapper.selectById(friend.getFriendId());
            if (user != null) {
                FriendVO friendVO = FriendVO.builder()
                        .userId(user.getUserId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .Ip(user.getLoginIp())
                        .loginStatus(user.getLoginStatus())
                        .remark(friend.getRemark())
                        .createdAt(friend.getCreatedAt())
                        .personalSignature("")
                        .build();
                
                // 如果头像URL是相对路径，转换为完整URL
                if (friendVO.getAvatarUrl() != null && !friendVO.getAvatarUrl().startsWith("http")) {
                    friendVO.setAvatarUrl(ossProperties.getOssDomainPrefix() + friendVO.getAvatarUrl());
                }
                
                // 获取好友的个人签名
                QueryWrapper<UserSettings> settingsWrapper = new QueryWrapper<>();
                settingsWrapper.eq("user_id", friend.getFriendId());
                UserSettings userSettings = userSettingsMapper.selectOne(settingsWrapper);
                
                if (userSettings != null) {
                    friendVO.setPersonalSignature(userSettings.getPersonalSignature());
                }
                
                friendList.add(friendVO);
            }
        }
        
        return friendList;
    }
    
    @Override
    public List<UserBasicVO> getFriendRequests(String userId) {
        // 查询发送给用户的所有待处理好友请求
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.eq("friend_id", userId).eq("status", "PENDING");
        List<Friend> requests = friendMapper.selectList(wrapper);
        
        List<UserBasicVO> requestList = new ArrayList<>();
        
        // 处理好友请求
        for (Friend request : requests) {
            User user = userMapper.selectById(request.getUserId());
            if (user != null) {
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
                
                requestList.add(userBasicVO);
            }
        }
        
        return requestList;
    }
    
    @Override
    public boolean deleteFriend(String userId, String friendId) {
        // 删除好友关系 (两个方向)
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
    
    // 添加备注功能
    public boolean updateFriendRemark(String userId, String friendId, String remark) {
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        
        // 查找用户给好友的备注
        wrapper.eq("user_id", userId).eq("friend_id", friendId);
        Friend friend = friendMapper.selectOne(wrapper);
        
        if (friend != null) {
            friend.setRemark(remark);
            friend.setUpdatedAt(System.currentTimeMillis());
            friendMapper.update(friend, wrapper);
            return true;
        }
        
        return false;
    }
    
    @Override
    public List<UserBasicVO> getSentFriendRequests(String userId) {
        // 查询用户发出的所有待处理好友请求
        QueryWrapper<Friend> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("status", "PENDING");
        List<Friend> requests = friendMapper.selectList(wrapper);
        
        List<UserBasicVO> requestList = new ArrayList<>();
        
        // 处理好友请求
        for (Friend request : requests) {
            User user = userMapper.selectById(request.getFriendId());
            if (user != null) {
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
                
                requestList.add(userBasicVO);
            }
        }
        
        return requestList;
    }
}