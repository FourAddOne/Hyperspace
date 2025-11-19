package com.lihuahua.hyperspace.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "好友VO", description = "用于好友信息展示的视图对象")
public class FriendVO {
    
    @Schema(description = "用户1ID（较小的用户ID）")
    private String userOne;
    
    @Schema(description = "用户2ID（较大的用户ID）")
    private String userSec;
    
    @Schema(description = "好友状态 (PENDING: 等待确认, ACCEPTED: 已接受, REJECTED: 已拒绝, BLOCKED: 已屏蔽)")
    private String status;
    
    @Schema(description = "屏蔽状态 (NONE: 无屏蔽, USER_ONE_BLOCKED: 用户1屏蔽了用户2, USER_SEC_BLOCKED: 用户2屏蔽了用户1, BOTH_BLOCKED: 双方互相屏蔽)")
    private String blockStatus;
    
    @Schema(description = "用户1对用户2的备注名称")
    private String userOneRemark;
    
    @Schema(description = "用户2对用户1的备注名称")
    private String userSecRemark;
    
    @Schema(description = "创建时间")
    private Long createdAt;
    
    @Schema(description = "更新时间")
    private Long updatedAt;
}