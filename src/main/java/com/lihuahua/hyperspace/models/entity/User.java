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
        System.out.println("User.login - 输入的密码: " + inputPassword);
        
        // 使用统一的密码验证方法
        Boolean result = PasswordUtil.checkPw(inputPassword, this.password);
        System.out.println("User.login - 数据库中的密码: " + this.password);
        System.out.println("User.login - 密码比较结果: " + result);
        
        return result;
    }

    public Boolean logout() {
        loginStatus = false;
        return true;
    }

    public Boolean updateAvatar(String newAvatarUrl) {
        avatarUrl = newAvatarUrl;
        return true;
    }

    public Boolean updateLastReadTs(Long ts) {
        lastReadTs = ts;
        return true;
    }
    
    public void setLoginStatus(Boolean loginStatus) {
        this.loginStatus = loginStatus;
    }
    
    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }
    
    public String getLoginIp() {
        return loginIp;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    /**
     * 设置密码时进行加密
     * @param password 原始密码
     */
    public void setPassword(String password) {
        this.password = password;  // 直接设置密码，不进行加密
    }
    
    /**
     * 加密并设置密码
     * @param rawPassword 原始密码
     */
    public void setEncryptedPassword(String rawPassword) {
        this.password = PasswordUtil.encrypt(rawPassword);
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public void setRegisterIp(String registerIp) {
        this.registerIp = registerIp;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }

}