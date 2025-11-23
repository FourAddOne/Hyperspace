package com.lihuahua.hyperspace.controller.user;

import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户注册接口")
@RestController
@RequestMapping("/user")
public class RegisterController {

    @Resource
    private UserServer userServer;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ResVO<UserLoginVO> register(@RequestBody RegisterDTO user) {
        Map<String, String> credential = new HashMap<>();
        credential.put("username", user.getUserName());
        credential.put("password", user.getPassword());
        credential.put("email", user.getEmail());
        credential.put("ip", user.getIp());

        try {
            Boolean result = userServer.register(credential);
            if (result) {
                // 注册成功后自动登录
                Map<String, String> loginCredential = new HashMap<>();
                loginCredential.put("email", user.getEmail());
                loginCredential.put("password", user.getPassword());
                loginCredential.put("ip", user.getIp());
                
                UserLoginVO resUser = userServer.login(loginCredential);
                return ResVO.success(resUser);
            } else {
                return ResVO.fail("注册失败");
            }
        } catch (Exception e) {
            return ResVO.fail(e.getMessage());
        }
    }
}