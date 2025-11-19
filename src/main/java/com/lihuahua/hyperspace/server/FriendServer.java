package com.lihuahua.hyperspace.server;

import com.lihuahua.hyperspace.models.entity.Friend;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;

import java.util.List;

public interface FriendServer {
    
    // 发送好友请求
    boolean sendFriendRequest(String userId, String friendId);
    
    // 接受好友请求
    boolean acceptFriendRequest(String userId, String requesterId);
    
    // 拒绝好友请求
    boolean rejectFriendRequest(String userId, String requesterId);
    
    // 获取好友列表
    List<UserLoginVO> getFriendList(String userId);
    
    // 获取好友申请列表
    List<UserLoginVO> getFriendRequests(String userId);
    
    // 删除好友
    boolean deleteFriend(String userId, String friendId);
    
    // 检查是否为好友
    boolean isFriend(String userId, String friendId);
}