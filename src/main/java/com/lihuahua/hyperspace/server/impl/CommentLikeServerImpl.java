package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lihuahua.hyperspace.mapper.CommentLikeMapper;
import com.lihuahua.hyperspace.models.entity.CommentLike;
import com.lihuahua.hyperspace.server.CommentLikeServer;
import org.springframework.stereotype.Service;

@Service
public class CommentLikeServerImpl extends ServiceImpl<CommentLikeMapper, CommentLike> implements CommentLikeServer {
}