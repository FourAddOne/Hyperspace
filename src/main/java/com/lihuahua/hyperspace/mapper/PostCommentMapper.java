package com.lihuahua.hyperspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lihuahua.hyperspace.models.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {
}