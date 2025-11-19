package com.lihuahua.hyperspace.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(name = "群组VO", description = "用于群组信息展示的视图对象")
public class GroupVO {
    
    @Schema(description = "群组唯一标识")
    private String groupId;
    
    @Schema(description = "群组名称")
    private String groupName;
    
    @Schema(description = "创建者ID")
    private String creatorId;
    
    @Schema(description = "群组状态 (NORMAL: 正常, MUTE_ALL: 全体禁言, BANNED: 封禁)")
    private String status;
    
    @Schema(description = "群组成员ID列表")
    private List<String> memberIds;
    
    @Schema(description = "创建时间")
    private Long createdAt;
    
    @Schema(description = "更新时间")
    private Long updatedAt;
}