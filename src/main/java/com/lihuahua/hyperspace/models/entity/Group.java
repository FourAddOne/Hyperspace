package com.lihuahua.hyperspace.models.entity;

import lombok.Data;
import java.util.List;

@Data
public class Group {
    
    /**
     * 群组唯一标识
     */
    private String groupId;
    
    /**
     * 群组名称
     */
    private String groupName;
    
    /**
     * 创建者ID
     */
    private String creatorId;
    
    /**
     * 群组状态 (NORMAL: 正常, MUTE_ALL: 全体禁言, BANNED: 封禁)
     */
    private String status;
    
    /**
     * 群组成员列表
     */
    private List<String> memberIds;
    
    /**
     * 创建时间
     */
    private Long createdAt;
    
    /**
     * 更新时间
     */
    private Long updatedAt;
}