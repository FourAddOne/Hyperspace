package com.lihuahua.hyperspace.models.dto;

import lombok.Data;

@Data
public class OssPolicyDTO {
    private String accessKeyId;
    private String policy;
    private String signature;
    private String dir;
    private String host;
    private String expire;
}