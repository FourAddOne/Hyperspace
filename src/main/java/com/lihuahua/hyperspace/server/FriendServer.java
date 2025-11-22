package com.lihuahua.hyperspace.server;

import com.lihuahua.hyperspace.models.entity.Friend;
import com.lihuahua.hyperspace.models.vo.FriendVO;
import com.lihuahua.hyperspace.models.vo.UserBasicVO;

import java.util.List;

public interface FriendServer {
    
    // 发送好友请求
    boolean sendFriendRequest(String userId, String friendId);
    
    // 接受好友请求
    boolean acceptFriendRequest(String userId, String requesterId);
    
    // 拒绝好友请求
    boolean rejectFriendRequest(String userId, String requesterId);
    
    // 屏蔽用户（拒绝并阻止其再次发送请求）
    boolean blockFriendRequest(String userId, String requesterId);
    
    // 获取好友列表
    List<FriendVO> getFriendList(String userId);
    
    // 获取好友申请列表
    List<UserBasicVO> getFriendRequests(String userId);
    
    // 删除好友
    boolean deleteFriend(String userId, String friendId);
    
    // 检查是否为好友
    boolean isFriend(String userId, String friendId);
    
    // 更新好友备注
    boolean updateFriendRemark(String userId, String friendId, String remark);
    
    // 获取用户发出的好友请求
    List<UserBasicVO> getSentFriendRequests(String userId);
}