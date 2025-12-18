package com.lihuahua.hyperspace.models.dto;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.Data;

@Data
@Tag(name = "登录参数")
public class LoginDTO {

    @Parameter(name = "用户ID",description = "用户ID(与邮箱二选一)")
    private String userId;
    @Parameter(name = "邮箱",description = "用户邮箱(与用户ID二选一)")
    private String email;
    @Parameter(name = "密码",description = "用户密码,注销时可不传")
    private String password;
    @Schema(description = "IP地址")
    private String ip; // 修复字段名，从前端传来的应该是 "ip" 而不是 "Ip"

}