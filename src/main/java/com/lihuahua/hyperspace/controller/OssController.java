package com.lihuahua.hyperspace.controller;

import com.lihuahua.hyperspace.models.dto.OssPolicyDTO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.utils.OssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oss")
@Tag(name = "OSS相关接口")
public class OssController {
    
    @Autowired
    private OssUtil ossUtil;
    
    @Operation(summary = "获取OSS上传策略")
    @GetMapping("/policy")
    public ResVO<OssPolicyDTO> getOssPolicy(@RequestParam(required = false, defaultValue = "uploads") String fileType) {
        try {
            OssPolicyDTO policy = ossUtil.generatePolicy(fileType);
            return ResVO.success(policy);
        } catch (Exception e) {
            return ResVO.fail("获取OSS上传策略失败: " + e.getMessage());
        }
    }
}