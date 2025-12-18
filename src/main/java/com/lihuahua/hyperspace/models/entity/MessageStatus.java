package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("message_status")
public class MessageStatus {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("message_id")
    private String messageId;
    
    @TableField("user_id")
    private String userId;
    
    @TableField("status")
    private String status; // delivered, read
    
    @TableField("updated_at")
    private Date updatedAt;
}