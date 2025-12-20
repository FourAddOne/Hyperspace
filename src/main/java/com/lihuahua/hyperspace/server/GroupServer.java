package com.lihuahua.hyperspace.server;

import com.lihuahua.hyperspace.models.dto.GroupDTO;

import java.util.List;

public interface GroupServer {

    /**
     * 创建群组
     * @param groupDTO 群组信息
     */
    void createGroup(GroupDTO groupDTO);

    /**
     * 加入群组
     * @param groupDTO 群组信息
     */
    void joinGroup(GroupDTO groupDTO);

    /**
     * 退出群组
     * @param groupDTO 群组信息
     */
    void quitGroup(GroupDTO groupDTO);
    
    /**
     * 踢出成员
     * @param groupDTO 群组信息
     */
    void kickMember(GroupDTO groupDTO);
    
    /**
     * 解散群组
     * @param groupDTO 群组信息
     */
    void disbandGroup(GroupDTO groupDTO);

    /**
     * 群组消息
     * @param groupDTO 群组信息
     */
    void groupMessage(GroupDTO groupDTO);

    /**
     * 群组列表
     * @param groupDTO 群组信息
     * @return 群组列表
     */
    List<GroupDTO> groupList(GroupDTO groupDTO);

    /**
     * 群组用户列表
     * @param groupDTO 群组信息
     */
    void groupUserList(GroupDTO groupDTO);
    
    /**
     * 搜索群组
     * @param keyword 搜索关键词
     * @return 匹配的群组列表
     */
    List<GroupDTO> searchGroups(String keyword, String userId);
    
    /**
     * 根据群组ID获取群组信息
     * @param groupId 群组ID
     * @return 群组信息
     */
    GroupDTO getGroupById(String groupId);
    
    /**
     * 获取群组消息列表
     * @param groupId 群组ID
     * @return 消息列表
     */
    List<GroupDTO> getGroupMessages(String groupId);
    
    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @return 成员列表
     */
    List<GroupDTO> getGroupMembers(String groupId);
}