package com.lihuahua.hyperspace.models.entity;

import lombok.Data;

@Data
public class Friend {
    
    /**
     * 用户1ID（较小的用户ID）
     */
    private String userOne;
    
    /**
     * 用户2ID（较大的用户ID）
     */
    private String userSec;
    
    /**
     * 好友状态 (PENDING: 等待确认, ACCEPTED: 已接受, REJECTED: 已拒绝, BLOCKED: 已屏蔽)
     */
    private String status;
    
    /**
     * 屏蔽状态 (NONE: 无屏蔽, USER_ONE_BLOCKED: 用户1屏蔽了用户2, USER_SEC_BLOCKED: 用户2屏蔽了用户1, BOTH_BLOCKED: 双方互相屏蔽)
     */
    private String blockStatus;
    
    /**
     * 用户1对用户2的备注名称
     */
    private String userOneRemark;
    
    /**
     * 用户2对用户1的备注名称
     */
    private String userSecRemark;
    
    /**
     * 创建时间
     */
    private Long createdAt;
    
    /**
     * 更新时间
     */
    private Long updatedAt;
}