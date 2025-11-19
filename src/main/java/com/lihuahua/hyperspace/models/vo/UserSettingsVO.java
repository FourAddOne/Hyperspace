package com.lihuahua.hyperspace.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "用户设置信息")
public class UserSettingsVO {
    
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "暗色模式")
    private Boolean darkMode;
    
    @Schema(description = "背景图片")
    private String backgroundImage;
    
    @Schema(description = "背景图片透明度")
    private Integer backgroundOpacity;
    
    @Schema(description = "布局模式")
    private String layout;
    
    @Schema(description = "个人签名")
    private String personalSignature;
    
    @Schema(description = "性别")
    private String gender;
    
    @Schema(description = "年龄")
    private Integer age;
    
    @Schema(description = "创建时间")
    private Date createdAt;
    
    @Schema(description = "更新时间")
    private Date updatedAt;
}