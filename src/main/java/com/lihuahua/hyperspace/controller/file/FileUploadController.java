package com.lihuahua.hyperspace.controller.file;

import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.utils.LocalFileUtil;
import com.lihuahua.hyperspace.utils.OssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "文件上传接口")
@RestController
@RequestMapping("/file")
public class FileUploadController {

    @Resource
    private LocalFileUtil localFileUtil;

    @Resource
    private OssUtil ossUtil;

    // 配置属性，决定使用哪种上传方式
    @Value("${file.upload.mode:local}")
    private String uploadMode;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ResVO<String> uploadFile(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "fileType", defaultValue = "other") String fileType) {
        try {
            System.out.println("开始上传文件: " + file.getOriginalFilename() + ", 文件类型: " + fileType);
            System.out.println("上传模式: " + uploadMode);
            
            String fileUrl;
            if ("oss".equals(uploadMode)) {
                // 使用OSS上传（云端）
                System.out.println("使用OSS上传");
                String filePath = ossUtil.uploadFileToOSS(file, fileType);
                // 对于头像和背景图片，使用公开URL；其他文件使用预签名URL
                if ("avatar".equals(fileType) || "background".equals(fileType)) {
                    fileUrl = ossUtil.generatePresignedUrl(filePath,8*3600);
                } else {
                    fileUrl = ossUtil.generatePresignedUrl(filePath, 3600); // 1小时有效期
                }
            } else {
                // 使用本地上传
                System.out.println("使用本地上传");
                fileUrl = localFileUtil.uploadLocalFile(file);
                System.out.println("本地上传完成，文件URL: " + fileUrl);
            }
            System.out.println("文件上传成功，URL: " + fileUrl);
            return ResVO.success(fileUrl);
        } catch (IOException e) {
            System.err.println("文件上传失败(IOException): " + e.getMessage());
            e.printStackTrace();
            return ResVO.fail("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("文件上传异常(Exception): " + e.getMessage());
            e.printStackTrace();
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