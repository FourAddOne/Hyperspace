package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.enums.LoginFail;
import com.lihuahua.hyperspace.enums.RegisterFail;
import com.lihuahua.hyperspace.exception.LoginException;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.server.UserServer;
import com.lihuahua.hyperspace.utils.OssUtil;
import com.lihuahua.hyperspace.utils.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServerImpl implements UserServer {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OssUtil ossUtil;

    private final Logger logger = LoggerFactory.getLogger(UserServerImpl.class);


    @Override
    public Boolean login(LoginDTO user) {
        QueryWrapper<User> queryWrapper =generateQueryWrapperByIdOrEmail(user);
        if(userMapper.selectOne(queryWrapper) == null){
            throw new LoginException(LoginFail.USER_NOT_EXIST.getMessage());
        }
        Boolean result = PasswordUtil.checkPw(user.getPassword(), userMapper.selectOne(queryWrapper).getPassword());
        if(result){

            //TODO 登录后将建立socket连接,并保存到redis中



            User user_update=userMapper.selectOne(queryWrapper);
            user_update.setLoginIp(user.getIp());
            user_update.setLoginStatus(true);
            userMapper.update(user_update, queryWrapper);

        }else {
            throw new LoginException(LoginFail.PASSWORD_ERROR.getMessage());
        }
        return true;
    }



    @Override
    public Boolean register(RegisterDTO registerUser) {


       QueryWrapper <User> queryWrapper = new QueryWrapper<>();


       //TODO 手机号注册
       queryWrapper.eq("email", registerUser.getEmail());

       if(userMapper.selectOne(queryWrapper) != null){
           throw new LoginException(RegisterFail.EMAIL_EXIST.getMessage());
       }

       User newUser = new User();
       // 生成随机字符串后缀
       String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        if("".equals(registerUser.getUserName())){
            newUser.setUserName("用户" + randomSuffix);
        }else {
            newUser.setUserName(registerUser.getUserName());
        }

       newUser.setAvatarUrl("avatar/default/avatar0.png");

       newUser.setEmail(registerUser.getEmail());
       newUser.setPassword(PasswordUtil.encrypt(registerUser.getPassword()));
       newUser.setRegisterIp(registerUser.getIp());

        int result =userMapper.insert(newUser);


        return result>=1;
    }

    @Override
    public User getPersionInfo(LoginDTO loginUser) {
        QueryWrapper<User> queryWrapper = generateQueryWrapperByIdOrEmail(loginUser);

        User user = userMapper.selectOne(queryWrapper);
        if(user != null) {
            user.setAvatarUrl(ossUtil.generatePresignedUrl(user.getAvatarUrl(), 60 * 60 * 24 * 365));
            user.setPassword("加密中");
        }else {
            throw new LoginException(LoginFail.USER_NOT_EXIST.getMessage());
        }
        return user;
    }

    @Override
    public Boolean logout(LoginDTO loginUser) {

        QueryWrapper<User> queryWrapper = generateQueryWrapperByIdOrEmail(loginUser);

        User user_update=userMapper.selectOne(queryWrapper);
        if(user_update.getLoginStatus()!= true){
            throw new LoginException(LoginFail.USER_NOT_LOGIN.getMessage());
        }
        user_update.setLoginStatus(false);
        int result = userMapper.update(user_update, queryWrapper);

        //TODO 断开socket连接





        return result >= 1;
    }

    private QueryWrapper<User> generateQueryWrapperByIdOrEmail(LoginDTO user) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        if("".equals(user.getEmail()) && "".equals(user.getUserId())){
            throw new LoginException(LoginFail.USER_ID_OR_EMAIL_IS_EMPTY.getMessage());
        }
        if(user.getUserId() != null&& !user.getUserId().isEmpty()){
            queryWrapper.eq("user_id", user.getUserId());
        }else {
            queryWrapper.eq("email", user.getEmail());
        }
        return queryWrapper;
    }
}