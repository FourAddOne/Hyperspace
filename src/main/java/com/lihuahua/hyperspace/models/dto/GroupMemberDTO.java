package com.lihuahua.hyperspace.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "群成员DTO", description = "用于群成员操作的数据传输对象")
public class GroupMemberDTO {
    
    @Schema(description = "群组ID")
    private String groupId;
    
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "用户角色(MEMBER, ADMIN, OWNER)")
    private String role;
    
    @Schema(description = "用户状态 (NORMAL: 正常, MUTED: 禁言)")
    private String status;
    
    @Schema(description = "禁言结束时间 (为NULL表示未禁言，-1表示永久禁言)")
    private Long muteEndTime;
}