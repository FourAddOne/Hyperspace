package com.lihuahua.hyperspace.controller.user;


import com.aliyun.oss.OSS;
import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户登录控制器
 * 处理用户登录和注销相关的HTTP请求
 */
@RestController("loginUserController")
@RequestMapping("/user")
@Tag(name = "用户登录模块", description = "处理用户登录和注销相关接口")
public class loginController {

    /**
     * 用户服务类，处理用户相关的业务逻辑
     */
    @Autowired
    private UserService userServer;

    /**
     * 阿里云OSS客户端，用于文件存储操作
     */
    @Autowired
    private OSS ossClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 日志记录器，用于记录控制器中的操作日志
     */
    private final Logger logger = LoggerFactory.getLogger(loginController.class);

    /**
     * 用户登录接口
     * 
     * @param loginUser 包含用户名和密码的登录信息DTO
     * @return 登录结果，成功时返回用户信息和token，失败时返回错误信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "根据用户名和密码进行用户身份验证")
    public ResVO login(@RequestBody LoginDTO loginUser) {

        try{

            UserLoginVO userLoginVO = userServer.login(loginUser);
            if(userLoginVO != null) {
                return ResVO.success(userLoginVO);
            }else  {
                throw new Exception("登录失败,未知原因");
            }
        }catch (Exception e){
            return ResVO.fail(e.getMessage());
        }

    }

    /**
     * 用户注销接口
     * 
     * @param user 包含用户信息的DTO
     * @return 注销结果，成功或失败信息
     */
    @PostMapping("/logout")
    @Operation(summary = "用户注销", description = "用户退出登录状态")
    public ResVO logout(@RequestBody LoginDTO user) {
        try{
            // 调用用户服务进行注销操作
            Boolean result = userServer.logout(user);



            if(result){
                return ResVO.success("注销成功");
            }else{
                return ResVO.fail("注销失败");
            }
        }catch (Exception e){
            return ResVO.fail(e.getMessage());
        }


    }
}
