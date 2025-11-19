package com.lihuahua.hyperspace.models.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("\"user\"")
public class UserPO {

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
}