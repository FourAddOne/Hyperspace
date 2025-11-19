package com.lihuahua.hyperspace.mapper;

import com.aliyuncs.ram.model.v20150501.CreateUserResponse;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihuahua.hyperspace.models.po.UserPO;
import com.lihuahua.hyperspace.models.pojo.UserInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;

@Mapper
public interface UserMapper extends BaseMapper<UserPO> {

    UserPO getUserByUserName(String username);

    UserInfo getUserInfoById(String userId);

    void updateLastMessageTime(String userId, Date date);
}
