package com.lihuahua.hyperspace.models.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("\"friend\"")
public class FriendPO {
    
    /**
     * 数据库自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户1ID（较小的用户ID）
     */
    @TableField("user_one")
    private String userOne;
    
    /**
     * 用户2ID（较大的用户ID）
     */
    @TableField("user_sec")
    private String userSec;
    
    /**
     * 好友状态 (PENDING: 等待确认, ACCEPTED: 已接受, REJECTED: 已拒绝, BLOCKED: 已屏蔽)
     */
    @TableField("status")
    private String status;
    
    /**
     * 屏蔽状态 (NONE: 无屏蔽, USER_ONE_BLOCKED: 用户1屏蔽了用户2, USER_SEC_BLOCKED: 用户2屏蔽了用户1, BOTH_BLOCKED: 双方互相屏蔽)
     */
    @TableField("block_status")
    private String blockStatus;
    
    /**
     * 用户1对用户2的备注名称
     */
    @TableField("user_one_remark")
    private String userOneRemark;
    
    /**
     * 用户2对用户1的备注名称
     */
    @TableField("user_sec_remark")
    private String userSecRemark;
    
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