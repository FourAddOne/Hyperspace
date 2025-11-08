package com.lihuahua.hyperspace.server;

import com.lihuahua.hyperspace.models.dto.LoginDTO;
import com.lihuahua.hyperspace.models.dto.RegisterDTO;
import com.lihuahua.hyperspace.models.entity.User;

public interface UserServer {

    Boolean login(LoginDTO  user);

    Boolean logout(LoginDTO user);

    Boolean register(RegisterDTO user);

    User getPersionInfo(LoginDTO user);
}
