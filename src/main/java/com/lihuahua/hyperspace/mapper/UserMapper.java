package com.lihuahua.hyperspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihuahua.hyperspace.models.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 移除了自定义的updateById方法，使用MyBatis Plus提供的方法
}