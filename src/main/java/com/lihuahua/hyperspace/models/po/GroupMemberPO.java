package com.lihuahua.hyperspace.models.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("\"group_member\"")
public class GroupMemberPO {
    
    /**
     * 数据库自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 群组ID
     */
    @TableField("group_id")
    private String groupId;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;
    
    /**
     * 用户角色(MEMBER, ADMIN, OWNER)
     */
    @TableField("role")
    private String role;
    
    /**
     * 用户状态 (NORMAL: 正常, MUTED: 禁言)
     */
    @TableField("status")
    private String status;
    
    /**
     * 禁言结束时间 (为NULL表示未禁言，-1表示永久禁言)
     */
    @TableField("mute_end_time")
    private Long muteEndTime;
    
    /**
     * 加入时间
     */
    @TableField("joined_at")
    private Long joinedAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private Long updatedAt;
}