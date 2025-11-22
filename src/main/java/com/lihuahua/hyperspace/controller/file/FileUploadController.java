package com.lihuahua.hyperspace.controller.file;

import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.utils.LocalFileUtil;
import com.lihuahua.hyperspace.utils.OssProperties;
import com.lihuahua.hyperspace.utils.OssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
@Tag(name = "文件上传接口")
public class FileUploadController {
    
    @Resource
    private LocalFileUtil localFileUtil;

    @Resource
    private OssUtil ossUtil;

    @Autowired
    private OssProperties ossProperties;
    
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ResVO<String> uploadFile(@RequestParam("file") MultipartFile file,
                                   @RequestParam(required = false, defaultValue = "oss") String uploadMode,
                                   @RequestParam(required = false, defaultValue = "other") String fileType) {
        try {
            log.info("开始文件上传: 文件名={}, 文件大小={} bytes, 上传模式={}, 文件类型={}",
                    file.getOriginalFilename(), file.getSize(), uploadMode, fileType);
            
            String fileUrl;
            if ("oss".equals(uploadMode)) {
                // 使用OSS上传（云端）
                log.info("使用OSS上传");
                String filePath = ossUtil.uploadFileToOSS(file, fileType);
                log.info("文件在OSS中的路径: {}", filePath);
                // 对于头像和背景图片，使用公开URL；其他文件使用预签名URL
                log.info("检查文件类型: '{}' 是否为头像或背景", fileType);
                if ("avatar".equals(fileType) || "background".equals(fileType)) {
                    log.info("生成公开URL");
                    fileUrl = ossUtil.generatePublicUrl(filePath);
                } else {
                    log.info("生成预签名URL");
                    fileUrl = ossUtil.generatePresignedUrl(filePath, 3600); // 1小时有效期
                }
                log.info("生成的公开URL: {}", fileUrl);
            } else {
                // 使用本地上传
                log.info("使用本地上传");
                fileUrl = localFileUtil.uploadLocalFile(file);
                log.info("本地上传完成，文件URL: {}", fileUrl);
            }
            log.info("文件上传成功，URL: {}", fileUrl);
            return ResVO.success(fileUrl);
        } catch (IOException e) {
            log.error("文件上传失败(IOException): {}", e.getMessage(), e);
            return ResVO.fail("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传异常(Exception): {}", e.getMessage(), e);
            return ResVO.fail("文件上传异常: " + e.getMessage());
        }
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public ResVO<Boolean> deleteFile(@RequestParam("fileUrl") String fileUrl) {
        try {
            boolean result = false;
            if (fileUrl.contains("oss") || fileUrl.contains("aliyuncs.com")) {
                // 删除OSS中的文件
                result = ossUtil.deleteFileFromOSS(fileUrl);
            } else if (fileUrl.startsWith("/uploads/")) {
                // 删除本地文件
                result = localFileUtil.deleteLocalFile(fileUrl);
            } else {
                return ResVO.fail("不支持删除该类型的文件");
            }
            
            if (result) {
                System.out.println("文件删除成功: " + fileUrl);
            } else {
                System.out.println("文件删除失败: " + fileUrl);
            }
            
            return result ? ResVO.success(true) : ResVO.fail("文件删除失败");
        } catch (Exception e) {
            System.err.println("文件删除异常: " + e.getMessage());
            e.printStackTrace();
            return ResVO.fail("文件删除异常: " + e.getMessage());
        }
    }
}