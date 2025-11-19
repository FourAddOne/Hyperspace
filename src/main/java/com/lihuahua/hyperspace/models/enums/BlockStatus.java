package com.lihuahua.hyperspace.models.enums;

/**
 * 屏蔽状态枚举
 */
public enum BlockStatus {
    /**
     * 无屏蔽
     */
    NONE,
    
    /**
     * 用户1屏蔽了用户2
     */
    USER_ONE_BLOCKED,
    
    /**
     * 用户2屏蔽了用户1
     */
    USER_SEC_BLOCKED,
    
    /**
     * 双方互相屏蔽
     */
    BOTH_BLOCKED
}