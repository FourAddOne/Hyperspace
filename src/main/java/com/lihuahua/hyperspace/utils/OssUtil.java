package com.lihuahua.hyperspace.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.*;
import com.lihuahua.hyperspace.models.dto.OssPolicyDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class OssUtil {

    @Autowired
    private OSS ossClient;

    @Autowired
    private OssProperties ossProperties;

    /**
     * 上传文件到OSS
     * @param file 要上传的文件
     * @return 返回文件在OSS中的路径
     */
    public String uploadFileToOSS(MultipartFile file) throws IOException {
        return uploadFileToOSS(file, "uploads");
    }

    /**
     * 上传文件到OSS，根据文件类型存储到不同的文件夹
     * @param file 要上传的文件
     * @param fileType 文件类型 (avatar-头像, background-聊天背景, message-聊天图片, file-聊天文件, cover-封面图片, comment-评论图片, 其他默认为uploads)
     * @return 返回文件在OSS中的路径
     */
    public String uploadFileToOSS(MultipartFile file, String fileType) throws IOException {
        // 生成唯一的文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 根据文件类型确定存储路径
        String folder = "uploads/";
        if ("avatar".equals(fileType)) {
            folder = "avatars/";
        } else if ("background".equals(fileType)) {
            folder = "backgrounds/";
        } else if ("message".equals(fileType)) {
            folder = "messages/";
        } else if ("file".equals(fileType)) {
            folder = "files/";
        } else if ("cover".equals(fileType)) {
            folder = "cover/"; // 封面图片存储在cover文件夹中
        } else if ("comment".equals(fileType)) {
            folder = "comments/"; // 评论图片存储在comments文件夹中
        }
        
        String uniqueFilename = folder + UUID.randomUUID().toString() + fileExtension;

        // 上传文件到OSS
        // 使用ObjectMetadata设置ACL权限
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setObjectAcl(CannedAccessControlList.PublicRead);
        PutObjectRequest putObjectRequest = new PutObjectRequest(ossProperties.getBucketName(), uniqueFilename, file.getInputStream(), metadata);
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
        URL signedUrl = ossClient.generatePresignedUrl(ossProperties.getBucketName(), filePath, expiration);

        // 将URL转换为字符串并返回
        return signedUrl.toString();
    }
    
    /**
     * 生成公开访问的URL（适用于公开读取的文件）
     * @param filePath 文件在OSS中的路径
     * @return 返回公开访问的URL
     */
    public String generatePublicUrl(String filePath) {
        // 构造公开访问的URL
        return ossProperties.getOssDomainPrefix() + filePath;
    }
    
    /**
     * 从OSS中删除文件
     * @param filePath 文件在OSS中的路径或公开URL
     * @return 删除是否成功
     */
    public boolean deleteFileFromOSS(String filePath) {
        try {
            // 从公开URL中提取文件路径
            String objectName = filePath;
            if (filePath.startsWith("http")) {
                // 提取路径部分，例如从 https://bucket.endpoint/avatars/filename 提取 avatars/filename
                String prefix = ossProperties.getOssDomainPrefix();
                if (filePath.startsWith(prefix)) {
                    objectName = filePath.substring(prefix.length());
                } else {
                    // 处理其他可能的URL格式
                    URL url = new URL(filePath);
                    String path = url.getPath();
                    // 移除开头的斜杠
                    if (path.startsWith("/")) {
                        objectName = path.substring(1);
                    } else {
                        objectName = path;
                    }
                }
            }
            
            // 删除OSS中的文件
            ossClient.deleteObject(ossProperties.getBucketName(), objectName);
            return true;
        } catch (OSSException oe) {
            System.err.println("OSS错误码: " + oe.getErrorCode());
            System.err.println("OSS错误信息: " + oe.getMessage());
            System.err.println("OSS请求ID: " + oe.getRequestId());
            System.err.println("OSS主机ID: " + oe.getHostId());
            return false;
        } catch (Exception e) {
            System.err.println("删除OSS文件时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 从文件URL中提取OSS对象名称
     * @param fileUrl 文件的完整URL
     * @return OSS对象名称
     */
    public String extractObjectNameFromUrl(String fileUrl) {
        try {
            if (fileUrl.startsWith("http")) {
                // 处理公开URL格式，例如 https://bucket.endpoint/folder/filename
                String prefix = ossProperties.getOssDomainPrefix();
                if (fileUrl.startsWith(prefix)) {
                    return fileUrl.substring(prefix.length());
                } else {
                    // 处理其他URL格式
                    URL url = new URL(fileUrl);
                    String path = url.getPath();
                    return path.startsWith("/") ? path.substring(1) : path;
                }
            } else {
                // 如果已经是对象名称，直接返回
                return fileUrl;
            }
        } catch (Exception e) {
            System.err.println("提取对象名称时出错: " + e.getMessage());
            return fileUrl; // 出错时返回原始URL
        }
    }
    
    /**
     * 将相对路径转换为完整URL
     * @param relativePath 相对路径，如avatars/xxx.png
     * @return 完整的可访问URL
     */
    public String convertToFullUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return relativePath;
        }
        
        // 如果已经是完整URL，直接返回
        if (relativePath.startsWith("http")) {
            return relativePath;
        }
        
        // 拼接完整URL
        return ossProperties.getOssDomainPrefix() + relativePath;
    }
    
    /**
     * 生成OSS上传策略，用于前端直传
     * @param fileType 文件类型
     * @return OssPolicyResult 包含上传策略的对象
     */
    public OssPolicyDTO generatePolicy(String fileType) {
        // 设置过期时间（例如1小时）
        long expireEndTime = System.currentTimeMillis() + 3600 * 1000;
        Date expiration = new Date(expireEndTime);
        
        // 根据文件类型确定存储路径
        String dir = "uploads/";
        if ("avatar".equals(fileType)) {
            dir = "avatars/";
        } else if ("background".equals(fileType)) {
            dir = "backgrounds/";
        } else if ("message".equals(fileType)) {
            dir = "messages/";
        } else if ("file".equals(fileType)) {
            dir = "files/";
        } else if ("cover".equals(fileType)) {
            dir = "cover/"; // 封面图片存储在cover文件夹中
        } else if ("comment".equals(fileType)) {
            dir = "comments/"; // 评论图片存储在comments文件夹中
        }
        
        // 构造策略
        String policyJson = "{\n" +
                "    \"expiration\": \"" + formatISO8601Date(expiration) + "\",\n" +
                "    \"conditions\": [\n" +
                "      {\"bucket\": \"" + ossProperties.getBucketName() + "\"},\n" +
                "      [\"starts-with\", \"$key\", \"" + dir + "\"],\n" +
                "      {\"success_action_status\": \"200\"}\n" +
                "    ]\n" +
                "  }";
        
        String policyBase64 = BinaryUtil.toBase64String(policyJson.getBytes(StandardCharsets.UTF_8));
        String signature = calculateSignature(ossProperties.getAccessKeySecret(), policyBase64);
        
        OssPolicyDTO result = new OssPolicyDTO();
        result.setAccessKeyId(ossProperties.getAccessKeyId());
        result.setPolicy(policyBase64);
        result.setSignature(signature);
        result.setDir(dir);
        result.setHost("https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint());
        result.setExpire(String.valueOf(expireEndTime / 1000));
        
        return result;
    }
    
    /**
     * 计算签名
     * @param keySecret AccessKeySecret
     * @param policyBase64 Base64编码的策略
     * @return 签名字符串
     */
    private String calculateSignature(String keySecret, String policyBase64) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] signData = mac.doFinal(policyBase64.getBytes(StandardCharsets.UTF_8));
            return BinaryUtil.toBase64String(signData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("计算签名失败", e);
        }
    }
    
    /**
     * 格式化日期为ISO8601格式
     * @param date 日期
     * @return ISO8601格式的日期字符串
     */
    private String formatISO8601Date(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return df.format(date);
    }
}