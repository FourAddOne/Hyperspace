package com.lihuahua.hyperspace;

import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.server.UserServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class HyperspaceApplicationTests {

    @Autowired
    private UserServer userServer;

    @Test
    void contextLoads() {
    }

    @Test
    void testRegister(){
        /*
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setEmail("3506058341@qq.com");
        registerDTO.setPassword("123456");
        registerDTO.setUserName("lihuahua");

        userServer.register(registerDTO);
        */
    }

    @Test
    void testLogin() {
        /*
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("3506058341@qq.com");
        loginDTO.setPassword("123456");
        Boolean result=userServer.login(loginDTO);
        System.out.println(result);
        */
    }

    @Test
    void testLogout(){
    }

}