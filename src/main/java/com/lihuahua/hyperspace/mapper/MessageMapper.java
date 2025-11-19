package com.lihuahua.hyperspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihuahua.hyperspace.models.po.MessagePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息Mapper接口
 * 用于消息相关的数据库操作
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessagePO> {
}