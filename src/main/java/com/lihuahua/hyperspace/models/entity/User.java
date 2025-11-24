package com.lihuahua.hyperspace.models.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lihuahua.hyperspace.utils.PasswordUtil;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
@TableName("user")
public class User {

    @TableId("user_id")
    @TableField("user_id")
    private String userId;
    @TableField("user_name")
    private String userName;
    @TableField("email")
    private String email;
    @TableField("avatar_url")
    private String avatarUrl;
    @TableField("password")
    private String password;

    @TableField("register_ip")
    private String registerIp;
    
    @TableField("login_ip")
    private String loginIp;

    @TableField("last_read_ts")
    private Long lastReadTs;
    @TableField("login_status")
    private Boolean loginStatus;
    
    @TableField("created_at")
    private Date createdAt;
    
    @TableField("updated_at")
    private Date updatedAt;

    public Boolean login(Map<String, String> credential) {
        String inputPassword = credential.get("password");
        // 使用统一的密码验证方法
        Boolean result = PasswordUtil.checkPw(inputPassword, this.password);
        return result;
    }
    
    /**
     * 加密并设置密码
     * @param rawPassword 原始密码
     */
    public void setEncryptedPassword(String rawPassword) {
        this.password = PasswordUtil.encrypt(rawPassword);
    }

}