package com.lihuahua.hyperspace.models.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Map;

@Data
@TableName("\"user\"")
public class User {


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


    public Boolean login(Map<String, String> credential) {
        return credential.get("password").equals(password);
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


}
