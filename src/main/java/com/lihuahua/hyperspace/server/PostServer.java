package com.lihuahua.hyperspace.server;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lihuahua.hyperspace.models.entity.Post;

import java.util.Map;

public interface PostServer extends IService<Post> {
    
    /**
     * 创建帖子
     * @param post 帖子信息
     * @return 创建结果
     */
    Map<String, Object> createPost(Post post);
    
    /**
     * 获取帖子列表
     * @param page 页码
     * @param size 每页数量
     * @param category 分类
     * @param type 类型
     * @return 帖子列表
     */
    Map<String, Object> getPosts(Integer page, Integer size, String category, String type);
    
    /**
     * 获取帖子详情
     * @param postId 帖子ID
     * @param userId 用户ID（可选）
     * @return 帖子详情
     */
    Map<String, Object> getPostDetail(String postId, String userId);
    
    /**
     * 点赞帖子
     * @param postId 帖子ID
     * @param userId 用户ID
     * @param username 用户名
     * @return 操作结果
     */
    boolean toggleLike(String postId, String userId, String username);
    
    /**
     * 点赞帖子（使用RabbitMQ异步处理）
     * @param postId 帖子ID
     * @param userId 用户ID
     * @param username 用户名
     * @return 操作结果
     */
    boolean toggleLikeWithRabbitMQ(String postId, String userId, String username);
}