package com.lihuahua.hyperspace.models.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("\"group\"")
public class GroupPO {
    
    /**
     * 数据库自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 群组唯一标识
     */
    @TableField("group_id")
    private String groupId;
    
    /**
     * 群组名称
     */
    @TableField("group_name")
    private String groupName;
    
    /**
     * 创建者ID
     */
    @TableField("creator_id")
    private String creatorId;
    
    /**
     * 群组状态 (NORMAL: 正常, MUTE_ALL: 全体禁言, BANNED: 封禁)
     */
    @TableField("status")
    private String status;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private Long createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private Long updatedAt;
}