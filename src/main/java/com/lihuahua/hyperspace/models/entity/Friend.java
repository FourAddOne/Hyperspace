package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("friend")
public class Friend {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private String userId;
    
    @TableField("friend_id")
    private String friendId;
    
    @TableField("status")
    private String status; // PENDING, ACCEPTED, REJECTED, BLOCKED
    
    @TableField("block_status")
    private String blockStatus; // NONE, USER_ONE_BLOCKED, USER_SEC_BLOCKED, BOTH_BLOCKED
    
    @TableField("user_remark")
    private String userRemark;
    
    @TableField("friend_remark")
    private String friendRemark;
    
    @TableField("created_at")
    private Long createdAt;
    
    @TableField("updated_at")
    private Long updatedAt;
}