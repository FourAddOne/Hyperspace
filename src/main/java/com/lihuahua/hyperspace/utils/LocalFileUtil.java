package com.lihuahua.hyperspace.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class LocalFileUtil {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    /**
     * 上传文件到本地
     * @param file 要上传的文件
     * @return 返回文件访问URL
     */
    public String uploadLocalFile(MultipartFile file) throws IOException {

        // 创建上传目录（如果不存在）
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // 保存文件
        Path filePath = uploadDir.resolve(uniqueFilename);
        System.out.println("文件保存路径: " + filePath.toAbsolutePath());
        Files.write(filePath, file.getBytes());

        // 返回文件访问URL
        String fileUrl = "/uploads/" + uniqueFilename;
        return fileUrl;
    }

    /**
     * 删除本地文件
     * @param fileUrl 文件URL
     * @return 删除是否成功
     */
    public boolean deleteLocalFile(String fileUrl) {
        try {
            String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadPath).resolve(filename);
            Files.deleteIfExists(filePath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}