package com.lihuahua.hyperspace.Service;

import com.aliyuncs.ram.model.v20150501.CreateUserResponse;
import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.models.entity.User;
import com.lihuahua.hyperspace.models.vo.UserLoginVO;

public interface UserService {

    UserLoginVO login(LoginDTO  user);

    Boolean logout(LoginDTO user);

    Boolean register(RegisterDTO user);

    User getPersionInfo(LoginDTO user);

}