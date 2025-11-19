package com.lihuahua.hyperspace.models.entity;

import lombok.Data;

@Data
public class GroupMember {
    
    /**
     * 群组ID
     */
    private String groupId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户角色(MEMBER, ADMIN, OWNER)
     */
    private String role;
    
    /**
     * 用户状态 (NORMAL: 正常, MUTED: 禁言)
     */
    private String status;
    
    /**
     * 禁言结束时间 (为NULL表示未禁言，-1表示永久禁言)
     */
    private Long muteEndTime;
    
    /**
     * 加入时间
     */
    private Long joinedAt;
    
    /**
     * 更新时间
     */
    private Long updatedAt;
}