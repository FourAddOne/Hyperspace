package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lihuahua.hyperspace.mapper.PostMediaMapper;
import com.lihuahua.hyperspace.models.entity.PostMedia;
import com.lihuahua.hyperspace.server.PostMediaServer;
import org.springframework.stereotype.Service;

@Service
public class PostMediaServerImpl extends ServiceImpl<PostMediaMapper, PostMedia> implements PostMediaServer {
}