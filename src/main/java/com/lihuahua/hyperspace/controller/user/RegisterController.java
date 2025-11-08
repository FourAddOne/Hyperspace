package com.lihuahua.hyperspace.controller.user;

import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.server.UserServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("registerUserController")
@RequestMapping("/user")
public class RegisterController {

    @Autowired
    private UserServer userServer;

/**
 * 处理用户注册请求的接口方法
 * @param user 包含用户注册信息的RegisterDTO对象
 * @return 返回注册结果信息字符串
 */
    @PostMapping("/register")
    public ResVO register(@RequestBody RegisterDTO  user) {
        try {
        // 调用服务层处理用户注册逻辑
            userServer.register(user);
        // 注册成功，返回成功信息
            return ResVO.success("注册成功");
        } catch (Exception e) {
        // 注册失败，返回失败信息
            return ResVO.fail(e.getMessage());
        }
    }
}
