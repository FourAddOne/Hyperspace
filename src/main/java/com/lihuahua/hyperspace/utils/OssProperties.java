package com.lihuahua.hyperspace.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OssProperties {
    
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;
    
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    
    /**
     * 获取OSS的完整域名前缀
     * @return https://bucketName.endpoint/
     */
    public String getOssDomainPrefix() {
        return "https://" + bucketName + "." + endpoint + "/";
    }
    
    /**
     * 获取bucket名称
     * @return bucket名称
     */
    public String getBucketName() {
        return bucketName;
    }
    
    /**
     * 获取endpoint
     * @return endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }
}