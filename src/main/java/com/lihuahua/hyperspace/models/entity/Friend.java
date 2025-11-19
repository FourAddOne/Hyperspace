package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("friends")
public class Friend {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private String userId;
    
    @TableField("friend_id")
    private String friendId;
    
    @TableField("status")
    private String status; // pending, accepted, blocked
    
    @TableField("created_at")
    private Date createdAt;
    
    @TableField("updated_at")
    private Date updatedAt;
}