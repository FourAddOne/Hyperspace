package com.lihuahua.hyperspace.models.entity;

import lombok.Data;

import java.util.Map;

@Data
public class User {

    private String userId;
    
    private String userName;
    
    private String email;
    
    private String avatarUrl;
    
    private String password;

    private String registerIp;
    
    private String loginIp;

    private Long lastReadTs;
    
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
