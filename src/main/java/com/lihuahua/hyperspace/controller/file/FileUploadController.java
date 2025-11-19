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
    public ResVO<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("开始上传文件: " + file.getOriginalFilename());
            System.out.println("上传模式: " + uploadMode);
            
            String fileUrl;
            if ("oss".equals(uploadMode)) {
                // 使用OSS上传（云端）
                System.out.println("使用OSS上传");
                String filePath = ossUtil.uploadFileToOSS(file);
                fileUrl = ossUtil.generatePresignedUrl(filePath, 3600); // 1小时有效期
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
        boolean result;
        if (fileUrl.startsWith("http") || fileUrl.contains("oss")) {
            // 删除云端文件（示例）
            result = true; // 实际需要调用OSS删除接口
        } else {
            // 删除本地文件
            result = localFileUtil.deleteLocalFile(fileUrl);
        }
        return result ? ResVO.success(true) : ResVO.fail("文件删除失败");
    }
}