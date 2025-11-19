package com.lihuahua.hyperspace.models.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

@Data
@Tag(name = "注册参数")
public class RegisterDTO {

    @Schema(description = "用户名")
    private String userName;
    
    @Schema(description = "密码")
    private String password;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "IP地址")
    @JsonProperty("Ip")
    private String Ip;
}