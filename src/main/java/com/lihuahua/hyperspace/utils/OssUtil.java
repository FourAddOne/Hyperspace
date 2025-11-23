package com.lihuahua.hyperspace.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.DeleteObjectsRequest;
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

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

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
     * @param fileType 文件类型 (avatar-头像, background-聊天背景, 其他默认为uploads)
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
        }
        
        String uniqueFilename = folder + UUID.randomUUID().toString() + fileExtension;

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
    
    /**
     * 生成公开访问的URL（适用于公开读取的文件）
     * @param filePath 文件在OSS中的路径
     * @return 返回公开访问的URL
     */
    public String generatePublicUrl(String filePath) {
        // 构造公开访问的URL
        return "https://" + bucketName + "." + endpoint + "/" + filePath;
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
                String prefix = "https://" + bucketName + "." + endpoint + "/";
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
            ossClient.deleteObject(bucketName, objectName);
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
                String prefix = "https://" + bucketName + "." + endpoint + "/";
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
}