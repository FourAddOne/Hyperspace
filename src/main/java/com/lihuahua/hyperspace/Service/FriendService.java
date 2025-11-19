package com.lihuahua.hyperspace.Service;

import com.lihuahua.hyperspace.models.dto.FriendDTO;
import com.lihuahua.hyperspace.models.entity.Friend;
import com.lihuahua.hyperspace.models.vo.FriendVO;

import java.util.List;

public interface FriendService {
    
    /**
     * 添加好友
     * @param friendDTO 好友信息
     * @return 好友VO对象
     */
    FriendVO addFriend(FriendDTO friendDTO);
    
    /**
     * 根据用户1ID和用户2ID获取好友关系
     * @param userOne 用户1ID
     * @param userSec 用户2ID
     * @return 好友对象
     */
    Friend getFriendById(String userOne, String userSec);
    
    /**
     * 更新好友关系状态
     * @param friendDTO 好友信息
     * @return 是否更新成功
     */
    Boolean updateFriendStatus(FriendDTO friendDTO);
    
    /**
     * 更新好友备注
     * @param friendDTO 好友信息
     * @return 是否更新成功
     */
    Boolean updateFriendRemark(FriendDTO friendDTO);
    
    /**
     * 删除好友关系
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否删除成功
     */
    Boolean deleteFriend(String userId, String friendId);
    
    /**
     * 获取用户的好友列表
     * @param userId 用户ID
     * @return 好友列表
     */
    List<FriendVO> getFriendList(String userId);
    
    /**
     * 搜索用户的好友
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @return 匹配的好友列表
     */
    List<FriendVO> searchFriends(String userId, String keyword);
    
    /**
     * 屏蔽好友
     * @param userId 当前用户ID
     * @param friendId 要屏蔽的好友ID
     * @return 是否屏蔽成功
     */
    Boolean blockFriend(String userId, String friendId);
    
    /**
     * 取消屏蔽好友
     * @param userId 当前用户ID
     * @param friendId 要取消屏蔽的好友ID
     * @return 是否取消屏蔽成功
     */
    Boolean unblockFriend(String userId, String friendId);
    
    /**
     * 检查用户是否被屏蔽
     * @param userId 当前用户ID
     * @param friendId 要检查的好友ID
     * @return 是否被屏蔽
     */
    Boolean isBlocked(String userId, String friendId);
    
    /**
     * 获取好友请求列表
     * @param userId 用户ID
     * @return 好友请求列表
     */
    List<FriendVO> getFriendRequests(String userId);
    
    /**
     * 处理好友请求
     * @param friendDTO 好友信息
     * @return 是否处理成功
     */
    Boolean handleFriendRequest(FriendDTO friendDTO);
}