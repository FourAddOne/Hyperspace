package com.lihuahua.hyperspace.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户详细信息")
public class UserProfileVO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "头像链接")
    private String avatarUrl;

    @Schema(description = "登录IP")
    private String Ip;

    @Schema(description = "个人签名")
    private String personalSignature;

    @Schema(description = "登录状态")
    private Boolean loginStatus;
}