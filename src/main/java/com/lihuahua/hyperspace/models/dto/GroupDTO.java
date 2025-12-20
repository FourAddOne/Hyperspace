package com.lihuahua.hyperspace.models.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GroupDTO {
    private String groupId;
    private String groupName;
    private String userId;
    private String role;
    private String message;
    private String members;
    private int memberCount;
    private String avatarUrl;
    private String description;
    private String creatorId;
    private String userName;
    // 消息相关字段
    private String messageId;
    private String type;
    private String fromUserId;
    private String textContent;
    private String imageUrls;
    private String fileUrls;
    private String fileName;
    private Long fileSize;
    private Long clientTimestamp;
    private Long serverTimestamp;
    private String status;
    private String quoteMessageId;
    private String quoteMessageSenderName;
    private String createTime;
    private String updateTime;
    // 用户头像URL
    private String userAvatarUrl;
    
    List<String> membersList = new ArrayList<>();
    public void addMember(String memberId) {
        membersList.add(memberId);
    }
    
    public String getMembers() {
        return members;
    }
    
    public void setMembers(String members) {
        this.members = members;
    }
    
    public List<String> getMembersList() {
        return membersList;
    }
    
    public void setMembersList(List<String> membersList) {
        this.membersList = membersList;
    }
}