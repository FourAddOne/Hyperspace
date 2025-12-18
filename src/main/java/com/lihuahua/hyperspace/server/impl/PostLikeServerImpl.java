package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lihuahua.hyperspace.mapper.PostLikeMapper;
import com.lihuahua.hyperspace.models.entity.PostLike;
import com.lihuahua.hyperspace.server.PostLikeServer;
import org.springframework.stereotype.Service;

@Service
public class PostLikeServerImpl extends ServiceImpl<PostLikeMapper, PostLike> implements PostLikeServer {
}