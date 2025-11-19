package com.lihuahua.hyperspace.models.pojo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息类
 * 用于表示消息发送者或接收者的基本信息
 */
@Data
public class UserInfo implements Serializable {
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户头像URL
     */
    private String avatarUrl;
    
    /**
     * 用户角色
     */
    private String role;
    
    /**
     * 创建用户信息对象的静态方法
     * @param userId 用户ID
     * @param username 用户名
     * @param avatarUrl 用户头像URL
     * @return UserInfo对象
     */
    public static UserInfo of(String userId, String username, String avatarUrl) {
        UserInfo userInfo = new UserInfo();
        userInfo.userId = userId;
        userInfo.username = username;
        userInfo.avatarUrl = avatarUrl;
        return userInfo;
    }
}