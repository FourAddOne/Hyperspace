package com.lihuahua.hyperspace.mapper;


import com.lihuahua.hyperspace.models.dto.GroupDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMapper {


/**
 * 创建群组的方法
 * @param groupName 群组名称
 * @param groupId 群组ID
 * @param userId 创建者用户ID
 * @param members 群组成员列表
 * @param avatarUrl 群组头像URL
 * @param description 群组描述
 */
    void createGroup(@Param("groupName") String groupName, 
                     @Param("groupId") String groupId, 
                     @Param("userId") String userId, 
                     @Param("members") String members,
                     @Param("avatarUrl") String avatarUrl,
                     @Param("description") String description);

/**
 * 加入群组的方法
 */
    void joinGroup(String groupId, String userId, String role);

/**
 * 退出群组的方法
 * @param groupId 群组ID，指定要退出的群组
 * @param userId 用户ID，指定执行退出操作的用户
 */
    void quitGroup(String groupId, String userId);

/**
 * 发送群组消息的方法
 * @param groupId 群组ID，用于标识特定的群组
 * @param userId 用户ID，用于标识发送消息的用户
 * @param message 要发送的消息内容
 */
    // 声明一个无返回值的groupMessage方法
    // 该方法接收三个参数：群组ID、用户ID和消息内容
    void groupMessage(String groupId, String userId, String message);

/**
 * 获取用户群组列表的方法
 * @param userId 用户ID，用于指定需要查询群组列表的用户
 */
    void groupList(String userId);

/**
 * 根据群组ID获取群组成员列表
 *
 * @param groupId 群组ID，用于标识特定的群组
 */
    void groupUserList(String groupId);

    List<GroupDTO> groupListbyUserId(String userId);
    
    // 根据群组ID查找群组
    GroupDTO getGroupById(String groupId);
    
    // 获取所有群组（用于搜索）
    List<GroupDTO> getAllGroups();
    
    // 获取所有群组详情（用于搜索）
    List<GroupDTO> getAllGroupsWithDetails();
    
    // 检查用户是否已在群组中
    boolean isUserInGroup(@Param("groupId") String groupId, @Param("userId") String userId);
    
    // 更新群组成员列表
    void updateGroupMembers(@Param("groupId") String groupId, @Param("members") String members);
    
    // 更新群组成员数量
    void updateGroupMemberCount(@Param("groupId") String groupId, @Param("memberCount") int memberCount);
    
    // 获取群组消息列表
    List<GroupDTO> getGroupMessages(@Param("groupId") String groupId);
    
    // 删除群组中的所有成员
    void removeAllMembers(@Param("groupId") String groupId);
    
    // 删除群组
    void deleteGroup(@Param("groupId") String groupId);
    
    // 获取群组成员详情
    List<GroupDTO> getGroupMembersWithDetails(@Param("groupId") String groupId);
}