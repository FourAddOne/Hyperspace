package com.lihuahua.hyperspace.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Date;

@Component
public class OssUtil {

    @Autowired
    private OSS ossClient;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    /**
     * 生成预签名URL的方法，用于获取临时访问权限的链接
     * @param filePath 文件在OSS中的路径
     * @param expireSeconds URL的有效期（秒）
     * @return 返回生成的预签名URL字符串
     */
    public String generatePresignedUrl(String filePath, long expireSeconds){

        // 计算过期时间：当前时间加上指定的秒数
        Date expiration  = new Date(System.currentTimeMillis() + expireSeconds * 1000);

        // 使用更直接的方式生成预签名URL
        URL signedUrl = ossClient.generatePresignedUrl(bucketName, filePath, expiration);

        // 将URL转换为字符串并返回
        return signedUrl.toString();
    }
}