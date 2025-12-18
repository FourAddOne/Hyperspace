package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("conversation_participants")
public class ConversationParticipant {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("conversation_id")
    private String conversationId;
    
    @TableField("user_id")
    private String userId;
    
    @TableField("joined_at")
    private Date joinedAt;
    
    @TableField("left_at")
    private Date leftAt;
}