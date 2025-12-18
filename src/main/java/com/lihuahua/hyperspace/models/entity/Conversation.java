package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("conversations")
public class Conversation {
    
    @TableId(value = "conversation_id")
    private String conversationId;
    
    @TableField("type")
    private String type; // private, group
    
    @TableField("name")
    private String name;
    
    @TableField("created_at")
    private Date createdAt;
    
    @TableField("updated_at")
    private Date updatedAt;
}