package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("friend")
public class Friend {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("user_one")
    private String userOne;
    
    @TableField("user_sec")
    private String userSec;
    
    @TableField("status")
    private String status; // PENDING, ACCEPTED, REJECTED, BLOCKED
    
    @TableField("block_status")
    private String blockStatus; // NONE, USER_ONE_BLOCKED, USER_SEC_BLOCKED, BOTH_BLOCKED
    
    @TableField("user_one_remark")
    private String userOneRemark;
    
    @TableField("user_sec_remark")
    private String userSecRemark;
    
    @TableField("created_at")
    private Long createdAt;
    
    @TableField("updated_at")
    private Long updatedAt;
}