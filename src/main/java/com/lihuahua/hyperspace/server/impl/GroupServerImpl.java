package com.lihuahua.hyperspace.server.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lihuahua.hyperspace.mapper.GroupMapper;
import com.lihuahua.hyperspace.models.dto.GroupDTO;
import com.lihuahua.hyperspace.server.GroupServer;
import com.lihuahua.hyperspace.utils.IdGenerator;
import com.lihuahua.hyperspace.utils.JacksonUtils;
import com.lihuahua.hyperspace.utils.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;


@Slf4j
@Service
public class GroupServerImpl implements GroupServer {
    @Autowired
    private GroupMapper groupMapper;
    
    @Autowired
    private IdGenerator idGenerator;
    
    @Autowired
    private OssUtil ossUtil;




    @Override
    @Transactional
    public void createGroup(GroupDTO groupDTO) {
        // 生成群组ID
        if (groupDTO.getGroupId() == null || groupDTO.getGroupId().isEmpty()) {
            groupDTO.setGroupId(idGenerator.generateUniqueGroupId());
        }

        groupDTO.addMember(groupDTO.getUserId());
        groupDTO.setMembers(JacksonUtils.toJson(groupDTO.getMembersList()));
        
        // 处理头像URL，只保存相对路径
        if (groupDTO.getAvatarUrl() != null && !groupDTO.getAvatarUrl().isEmpty()) {
            String relativePath = ossUtil.extractObjectNameFromUrl(groupDTO.getAvatarUrl());
            groupDTO.setAvatarUrl(relativePath);
        }
        
        try {
            groupMapper.createGroup(
                groupDTO.getGroupName(), 
                groupDTO.getGroupId(), 
                groupDTO.getUserId(), 
                groupDTO.getMembers(),
                groupDTO.getAvatarUrl(),
                groupDTO.getDescription()
            );
            groupMapper.joinGroup(groupDTO.getGroupId(), groupDTO.getUserId(),"OWNER");
        } catch (Exception e) {
            log.error("创建群组失败: ", e);
            throw new RuntimeException("创建群组失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void joinGroup(GroupDTO groupDTO) {
        // 检查用户是否已经在群组中
        if (groupMapper.isUserInGroup(groupDTO.getGroupId(), groupDTO.getUserId())) {
            throw new RuntimeException("用户已在群组中");
        }

        groupDTO.setRole("MEMBER");
        groupMapper.joinGroup(groupDTO.getGroupId(), groupDTO.getUserId(),groupDTO.getRole());
        
        // 更新group表中的members字段和member_count
        try {
            // 获取群组信息
            GroupDTO groupInfo = groupMapper.getGroupById(groupDTO.getGroupId());
            if (groupInfo != null) {
                List<String> membersList;
                if (groupInfo.getMembers() != null && !groupInfo.getMembers().isEmpty()) {
                    // 将members字段从JSON字符串转换为List
                    membersList = JacksonUtils.fromJson(groupInfo.getMembers(), new TypeReference<List<String>>() {});
                } else {
                    // 如果members字段为null或空，初始化为空列表
                    membersList = new ArrayList<>();
                }
                
                // 添加新成员到列表中（避免重复添加）
                if (!membersList.contains(groupDTO.getUserId())) {
                    membersList.add(groupDTO.getUserId());
                    // 将更新后的列表转换为JSON字符串并更新数据库
                    groupMapper.updateGroupMembers(groupDTO.getGroupId(), JacksonUtils.toJson(membersList));
                    
                    // 更新群组成员数量
                    groupMapper.updateGroupMemberCount(groupDTO.getGroupId(), membersList.size());
                }
            }
        } catch (Exception e) {
            log.error("更新群组成员列表失败: ", e);
            // 不中断主要流程，仅记录错误
        }
    }

    @Override
    @Transactional
    public void quitGroup(GroupDTO groupDTO) {
        // 从group_member表中删除用户
        groupMapper.quitGroup(groupDTO.getGroupId(), groupDTO.getUserId());
        
        // 更新group表中的members字段和member_count
        try {
            // 获取群组信息
            GroupDTO groupInfo = groupMapper.getGroupById(groupDTO.getGroupId());
            if (groupInfo != null) {
                List<String> membersList;
                if (groupInfo.getMembers() != null && !groupInfo.getMembers().isEmpty()) {
                    // 将members字段从JSON字符串转换为List
                    membersList = JacksonUtils.fromJson(groupInfo.getMembers(), new TypeReference<List<String>>() {});
                } else {
                    // 如果members字段为null或空，初始化为空列表
                    membersList = new ArrayList<>();
                }
                
                // 从成员列表中移除该用户（如果存在）
                if (membersList.remove(groupDTO.getUserId())) {
                    // 将更新后的列表转换为JSON字符串并更新数据库
                    groupMapper.updateGroupMembers(groupDTO.getGroupId(), JacksonUtils.toJson(membersList));
                    
                    // 更新群组成员数量
                    groupMapper.updateGroupMemberCount(groupDTO.getGroupId(), membersList.size());
                }
            }
        } catch (Exception e) {
            log.error("更新群组成员列表失败: ", e);
            // 不中断主要流程，仅记录错误
        }
    }
    
    @Override
    @Transactional
    public void kickMember(GroupDTO groupDTO) {
        // 从group_member表中删除用户
        groupMapper.quitGroup(groupDTO.getGroupId(), groupDTO.getUserId());
        
        // 更新group表中的members字段和member_count
        try {
            // 获取群组信息
            GroupDTO groupInfo = groupMapper.getGroupById(groupDTO.getGroupId());
            if (groupInfo != null) {
                List<String> membersList;
                if (groupInfo.getMembers() != null && !groupInfo.getMembers().isEmpty()) {
                    // 将members字段从JSON字符串转换为List
                    membersList = JacksonUtils.fromJson(groupInfo.getMembers(), new TypeReference<List<String>>() {});
                } else {
                    // 如果members字段为null或空，初始化为空列表
                    membersList = new ArrayList<>();
                }
                
                // 从成员列表中移除该用户（如果存在）
                if (membersList.remove(groupDTO.getUserId())) {
                    // 将更新后的列表转换为JSON字符串并更新数据库
                    groupMapper.updateGroupMembers(groupDTO.getGroupId(), JacksonUtils.toJson(membersList));
                    
                    // 更新群组成员数量
                    groupMapper.updateGroupMemberCount(groupDTO.getGroupId(), membersList.size());
                }
            }
        } catch (Exception e) {
            log.error("更新群组成员列表失败: ", e);
            // 不中断主要流程，仅记录错误
        }
    }
    
    @Override
    @Transactional
    public void disbandGroup(GroupDTO groupDTO) {
        // 删除群组中的所有成员
        groupMapper.removeAllMembers(groupDTO.getGroupId());
        
        // 删除群组本身
        groupMapper.deleteGroup(groupDTO.getGroupId());
    }

    @Override
    public void groupMessage(GroupDTO groupDTO) {
        groupMapper.groupMessage(groupDTO.getGroupId(), groupDTO.getUserId(), groupDTO.getMessage());
    }

    @Override
    public List<GroupDTO> groupList(GroupDTO groupDTO) {
        List<GroupDTO> groups = groupMapper.groupListbyUserId(groupDTO.getUserId());
        // 为每个群组的头像URL拼接完整路径
        for (GroupDTO group : groups) {
            if (group.getAvatarUrl() != null && !group.getAvatarUrl().isEmpty()) {
                group.setAvatarUrl(ossUtil.convertToFullUrl(group.getAvatarUrl()));
            }
            
            // 获取群主ID
            GroupDTO groupInfo = groupMapper.getGroupById(group.getGroupId());
            if (groupInfo != null) {
                group.setCreatorId(groupInfo.getCreatorId());
            }
        }
        return groups;
    }

    @Override
    public void groupUserList(GroupDTO groupDTO) {

    }
    
    /**
     * 搜索群组
     * @param keyword 搜索关键词
     * @return 匹配的群组列表
     */
    public List<GroupDTO> searchGroups(String keyword, String userId) {
        List<GroupDTO> allGroups = groupMapper.getAllGroupsWithDetails();
        // 过滤出用户未加入的群组，并根据群组名称或ID匹配
        List<GroupDTO> filteredGroups = allGroups.stream()
                .filter(group -> !groupMapper.isUserInGroup(group.getGroupId(), userId))
                .filter(group -> group.getGroupName().contains(keyword) || group.getGroupId().contains(keyword))
                .toList();
        
        // 为每个群组的头像URL拼接完整路径
        for (GroupDTO group : filteredGroups) {
            if (group.getAvatarUrl() != null && !group.getAvatarUrl().isEmpty()) {
                group.setAvatarUrl(ossUtil.convertToFullUrl(group.getAvatarUrl()));
            }
        }
        
        return filteredGroups;
    }
    
    /**
     * 根据群组ID获取群组信息
     * @param groupId 群组ID
     * @return 群组信息
     */
    public GroupDTO getGroupById(String groupId) {
        GroupDTO group = groupMapper.getGroupById(groupId);
        // 为群组的头像URL拼接完整路径
        if (group != null && group.getAvatarUrl() != null && !group.getAvatarUrl().isEmpty()) {
            group.setAvatarUrl(ossUtil.convertToFullUrl(group.getAvatarUrl()));
        }
        return group;
    }
    
    /**
     * 获取群组消息列表
     * @param groupId 群组ID
     * @return 消息列表
     */
    public List<GroupDTO> getGroupMessages(String groupId) {
        List<GroupDTO> messages = groupMapper.getGroupMessages(groupId);
        // 为每个消息的图片和文件URL拼接完整路径
        for (GroupDTO message : messages) {
            // 处理图片URL
            if (message.getImageUrls() != null && !message.getImageUrls().isEmpty()) {
                message.setImageUrls(ossUtil.convertToFullUrl(message.getImageUrls()));
            }
            
            // 处理文件URL
            if (message.getFileUrls() != null && !message.getFileUrls().isEmpty()) {
                message.setFileUrls(ossUtil.convertToFullUrl(message.getFileUrls()));
            }
            
            // 处理用户头像URL
            if (message.getUserAvatarUrl() != null && !message.getUserAvatarUrl().isEmpty()) {
                message.setUserAvatarUrl(ossUtil.convertToFullUrl(message.getUserAvatarUrl()));
            }
        }
        return messages;
    }
    
    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @return 成员列表
     */
    public List<GroupDTO> getGroupMembers(String groupId) {
        List<GroupDTO> members = groupMapper.getGroupMembersWithDetails(groupId);
        // 为每个成员的头像URL拼接完整路径
        for (GroupDTO member : members) {
            if (member.getAvatarUrl() != null && !member.getAvatarUrl().isEmpty()) {
                member.setAvatarUrl(ossUtil.convertToFullUrl(member.getAvatarUrl()));
            }
            
            // 确保必要的字段存在
            if (member.getUserName() == null || member.getUserName().isEmpty()) {
                member.setUserName("用户" + (member.getUserId() != null ? member.getUserId().substring(0, Math.min(8, member.getUserId().length())) : ""));
            }
        }
        return members;
    }
}