package com.lihuahua.hyperspace.models.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

@Data
@Tag(name = "注册参数")
public class RegisterDTO {

    @Schema(description = "用户名")
    private String userName; // 修正为驼峰命名法，与前端字段匹配
    
    @Schema(description = "密码")
    private String password;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "IP地址")
    private String ip; // 修正为小写，与前端字段匹配
}