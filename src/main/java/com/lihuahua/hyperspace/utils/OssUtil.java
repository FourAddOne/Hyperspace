package com.lihuahua.hyperspace.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Component
public class OssUtil {

    @Autowired
    private OSS ossClient;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    /**
     * 上传文件到OSS
     * @param file 要上传的文件
     * @return 返回文件在OSS中的路径
     */
    public String uploadFileToOSS(MultipartFile file) throws IOException {
        // 生成唯一的文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = "uploads/" + UUID.randomUUID().toString() + fileExtension;

        // 上传文件到OSS
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, uniqueFilename, file.getInputStream());
        ossClient.putObject(putObjectRequest);

        // 返回文件路径
        return uniqueFilename;
    }

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