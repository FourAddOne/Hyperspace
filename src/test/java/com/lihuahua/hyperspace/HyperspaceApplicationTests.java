package com.lihuahua.hyperspace;

import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.Service.UserService;
import com.lihuahua.hyperspace.utils.OssUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HyperspaceApplicationTests {

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private UserService userServer;

    @Test
    void contextLoads() {

//        System.out.println(PasswordUtil.encrypt("123456"));
//        String pw = PasswordUtil.encrypt("123456");
//        System.out.println(PasswordUtil.checkPw("12345",pw));

//        System.out.println(IdUtil.getId());

//        OssUtil ossUtil = new OssUtil();
        String url = ossUtil.generatePresignedUrl("avatar/default/avatar1.jpg", 100000);
        System.out.println(url);
    }

    @Test
    void testRegister(){

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setEmail("3506058341@qq.com");
        registerDTO.setPassword("123456");
        registerDTO.setUserName("lihuahua");

        userServer.register(registerDTO);
    }

    @Test
    void testLogin(){
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("3506058341@qq.com");
        loginDTO.setPassword("123456");
        Boolean result=userServer.login(loginDTO);
        System.out.println(result);
        System.out.println(userServer.getPersionInfo(loginDTO));
    }

    @Test
    void testLogout(){
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("3506058341@qq.com");
        loginDTO.setPassword("123456");
        Boolean result=userServer.logout(loginDTO);
        System.out.println(result);
    }

}