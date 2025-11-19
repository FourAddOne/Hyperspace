package com.lihuahua.hyperspace.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lihuahua.hyperspace.enums.LoginFail;
import com.lihuahua.hyperspace.enums.RegisterFail;
import com.lihuahua.hyperspace.exception.LoginException;
import com.lihuahua.hyperspace.mapper.UserMapper;
import com.lihuahua.hyperspace.mapper.convert.UserConvertMapper;
import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.po.UserPO;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;
import com.lihuahua.hyperspace.Service.UserService;
import com.lihuahua.hyperspace.utils.JwtTokenUtil;
import com.lihuahua.hyperspace.utils.OssUtil;
import com.lihuahua.hyperspace.utils.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userPoMapper;
    
    // 注入 MapStruct 映射器
    private final UserConvertMapper userConvertMapper = UserConvertMapper.INSTANCE;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);


    @Override
    public UserLoginVO login(LoginDTO user) {

        QueryWrapper<UserPO> queryWrapper =generateQueryWrapperByIdOrEmail(user);
        UserPO existingUser = userPoMapper.selectOne(queryWrapper);
        if(existingUser == null){
            throw new LoginException(LoginFail.USER_NOT_EXIST.getCode(), LoginFail.USER_NOT_EXIST.getMessage());
        }
        
        Boolean result = PasswordUtil.checkPw(user.getPassword(), existingUser.getPassword());
        if(result){

            //TODO 登录后将建立socket连接,并保存到redis中

            UserPO userUpdate = userPoMapper.selectOne(queryWrapper);
            userUpdate.setLoginIp(user.getIp());
            userUpdate.setLoginStatus(true);
            userPoMapper.update(userUpdate, queryWrapper);

            // Convert PO to Entity using MapStruct
            User userEntity = userConvertMapper.toEntity(userUpdate);

            UserLoginVO userLoginVO = UserLoginVO.builder()
                    .userId(userEntity.getUserId())
                    .userName(userEntity.getUserName())
                    .email(userEntity.getEmail())
                    .avatarUrl(userEntity.getAvatarUrl())
                    .accessToken(JwtTokenUtil.generateShortToken(userEntity.getUserId()))
                    .refreshToken(JwtTokenUtil.generateLongToken(userEntity.getUserId()))
                    .Ip(user.getIp()) // 添加 Ip 字段
                    .build();
            redisTemplate.opsForValue().set(
                    "hyperspace_user_AccessToken_"+userLoginVO.getUserId(),
                    userLoginVO.getAccessToken(),
                    60 * 60 ,
                    TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(
                    "hyperspace_user_RefreshToken_"+userLoginVO.getUserId(),
                    userLoginVO.getRefreshToken(),
                    60 * 60 * 24 * 365,
                    TimeUnit.SECONDS);

            return userLoginVO;

        }else {
            throw new LoginException(LoginFail.PASSWORD_ERROR.getCode(), LoginFail.PASSWORD_ERROR.getMessage());
        }

    }



    @Override
    public Boolean register(RegisterDTO registerUser) {


       QueryWrapper <UserPO> queryWrapper = new QueryWrapper<>();


       //TODO 手机号注册
       queryWrapper.eq("email", registerUser.getEmail());

       if(userPoMapper.selectOne(queryWrapper) != null){
           throw new LoginException(RegisterFail.EMAIL_EXIST.getCode(), RegisterFail.EMAIL_EXIST.getMessage());
       }

       UserPO newUser = userConvertMapper.toPO(registerUser);
       // 生成随机字符串后缀
       String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        if(registerUser.getUserName() == null || registerUser.getUserName().isEmpty()){
            newUser.setUserName("用户" + randomSuffix);
        }else {
            newUser.setUserName(registerUser.getUserName());
        }

       newUser.setAvatarUrl("avatar/default/avatar0.png");

       newUser.setEmail(registerUser.getEmail());
       newUser.setPassword(PasswordUtil.encrypt(registerUser.getPassword()));
       newUser.setRegisterIp(registerUser.getIp());

        int result =userPoMapper.insert(newUser);


        return result>=1;
    }

    @Override
    public User getPersionInfo(LoginDTO loginUser) {
        QueryWrapper<UserPO> queryWrapper = generateQueryWrapperByIdOrEmail(loginUser);

        UserPO userPO = userPoMapper.selectOne(queryWrapper);
        if(userPO != null) {
            // Convert PO to Entity using MapStruct
            User userEntity = userConvertMapper.toEntity(userPO);
            userEntity.setAvatarUrl(ossUtil.generatePresignedUrl(userEntity.getAvatarUrl(), 60 * 60 * 24 * 365));
            userEntity.setPassword("加密中");
            
            return userEntity;
        }else {
            throw new LoginException(LoginFail.USER_NOT_EXIST.getCode(), LoginFail.USER_NOT_EXIST.getMessage());
        }
    }

    @Override
    public Boolean logout(LoginDTO loginUser) {

        QueryWrapper<UserPO> queryWrapper = generateQueryWrapperByIdOrEmail(loginUser);

        // 获取用户当前的token并将其加入黑名单
        String accessToken = (String) redisTemplate.opsForValue().get("hyperspace_user_AccessToken_"+loginUser.getUserId());
        if (accessToken != null) {
            JwtTokenUtil.blacklistToken(accessToken);
        }
        
        String refreshToken = (String) redisTemplate.opsForValue().get("hyperspace_user_RefreshToken_"+loginUser.getUserId());
        if (refreshToken != null) {
            JwtTokenUtil.blacklistToken(refreshToken);
        }

        // 删除Redis中的token记录
        redisTemplate.delete("hyperspace_user_AccessToken_"+loginUser.getUserId());
        redisTemplate.delete("hyperspace_user_RefreshToken_"+loginUser.getUserId());

        UserPO userPO = userPoMapper.selectOne(queryWrapper);
        if(userPO.getLoginStatus()!= true){
            throw new LoginException(LoginFail.USER_NOT_LOGIN.getCode(), LoginFail.USER_NOT_LOGIN.getMessage());
        }
        
        userPO.setLoginStatus(false);
        int result = userPoMapper.update(userPO, queryWrapper);

        //TODO 断开socket连接

        return result >= 1;
    }

    private QueryWrapper<UserPO> generateQueryWrapperByIdOrEmail(LoginDTO user) {
        QueryWrapper<UserPO> queryWrapper = new QueryWrapper<>();

        if((user.getEmail() == null || user.getEmail().isEmpty()) && 
           (user.getUserId() == null || user.getUserId().isEmpty())){
            throw new LoginException(LoginFail.USER_ID_OR_EMAIL_IS_EMPTY.getCode(), LoginFail.USER_ID_OR_EMAIL_IS_EMPTY.getMessage());
        }
        if(user.getUserId() != null && !user.getUserId().isEmpty()){
            queryWrapper.eq("user_id", user.getUserId());
        }else {
            queryWrapper.eq("email", user.getEmail());
        }
        return queryWrapper;
    }
}