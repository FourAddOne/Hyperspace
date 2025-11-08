package com.lihuahua.hyperspace.models.vo;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginVO {

    private String userId;
    private String userName;
    private String email;
    private String avatarUrl;
    private String accessToken;
    private String refreshToken;
    private String Ip;



}
