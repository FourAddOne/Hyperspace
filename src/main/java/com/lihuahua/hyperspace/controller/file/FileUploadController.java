package com.lihuahua.hyperspace.controller.file;

import com.lihuahua.hyperspace.models.vo.ResVO;
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
    private OssUtil ossUtil;

    @Autowired
    private OssProperties ossProperties;
    
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ResVO<String> uploadFile(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "fileType", required = false, defaultValue = "other") String fileType) {
        try {
            log.info("开始文件上传: 文件名={}, 文件大小={} bytes, 文件类型={}",
                    file.getOriginalFilename(), file.getSize(), fileType);
            
            // 使用OSS上传（云端）
            log.info("使用OSS上传");
            String filePath = ossUtil.uploadFileToOSS(file, fileType);
            log.info("文件在OSS中的路径: {}", filePath);
            // 对于头像、背景和聊天图片，使用公开URL；其他文件使用公开URL（解决预签名URL过期问题）
            log.info("检查文件类型: '{}' 是否为头像、背景或聊天图片", fileType);
            String fileUrl;
            if ("avatar".equals(fileType) || "background".equals(fileType) || "message".equals(fileType)) {
                log.info("生成公开URL");
                fileUrl = ossUtil.generatePublicUrl(filePath);
            } else {
                // 修改：对于其他文件类型也使用公开URL，避免预签名URL过期问题
                log.info("生成公开URL（替代预签名URL）");
                fileUrl = ossUtil.generatePublicUrl(filePath);
            }
            log.info("生成的公开URL: {}", fileUrl);
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
            // 删除OSS中的文件
            boolean result = ossUtil.deleteFileFromOSS(fileUrl);
            
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