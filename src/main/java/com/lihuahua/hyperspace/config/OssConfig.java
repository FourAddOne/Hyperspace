package com.lihuahua.hyperspace.config;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.lihuahua.hyperspace.utils.OssProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {

    @Autowired
    private OssProperties ossProperties;

    @Bean
    public OSS OssClient() {
        return new OSSClientBuilder().build(
            ossProperties.getEndpoint(),
            ossProperties.getAccessKeyId(),
            ossProperties.getAccessKeySecret()
        );
    }
}