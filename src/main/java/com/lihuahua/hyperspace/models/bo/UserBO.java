package com.lihuahua.hyperspace.models.bo;

import lombok.Data;

@Data
public class UserBO {
    
    private String userId;
    
    private String userName;
    
    private String email;
    
    private String avatarUrl;
    
    private String password;

    private String registerIp;
    
    private String loginIp;

    private Long lastReadTs;
    
    private Boolean loginStatus;
}