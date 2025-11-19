package com.lihuahua.hyperspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihuahua.hyperspace.models.po.FriendPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FriendMapper extends BaseMapper<FriendPO> {

    // 查询用户作为 user_one 时的关联记录
    List<FriendPO> selectByUserOne(@Param("userId") String userId);

    // 查询用户作为 user_sec 时的关联记录
    List<FriendPO> selectByUserSec(@Param("userId") String userId);
}