package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.mapper.PostCommentMapper;
import com.lihuahua.hyperspace.models.entity.PostComment;
import com.lihuahua.hyperspace.server.PostCommentServer;
import com.lihuahua.hyperspace.server.PostLikeServer;
import com.lihuahua.hyperspace.service.CommentLikeRabbitMQService;
import com.lihuahua.hyperspace.models.entity.CommentLike;
import com.lihuahua.hyperspace.server.CommentLikeServer;
import com.lihuahua.hyperspace.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class PostCommentServerImpl extends ServiceImpl<PostCommentMapper, PostComment> implements PostCommentServer {
    
    @Autowired
    private CommentLikeRabbitMQService commentLikeRabbitMQService;
    
    @Autowired
    private CommentLikeServer commentLikeServer;
    
    @Autowired
    private PostLikeServer postLikeServer;
    
    @Autowired
    private OssUtil ossUtil;
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // ObjectMapper用于序列化和反序列化JSON
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 评论列表缓存前缀
    private static final String COMMENT_LIST_PREFIX = "post_comments:";
    // 评论点赞用户集合前缀
    private static final String COMMENT_LIKED_USERS_PREFIX = "comment_liked_users:";
    // 评论点赞数前缀
    private static final String COMMENT_LIKE_COUNT_PREFIX = "comment_like_count:";
    
    public PostCommentServerImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 根据帖子ID获取评论列表，优先从Redis缓存获取
     * @param postId 帖子ID
     * @return 评论列表
     */
    public List<PostComment> getCommentsByPostId(String postId) {
        String cacheKey = COMMENT_LIST_PREFIX + postId;
        
        // 尝试从Redis获取缓存
        String cachedCommentsJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedCommentsJson != null) {
            // 缓存命中，直接返回缓存数据
            try {
                // 反序列化评论列表
                List<PostComment> comments = objectMapper.readValue(
                    cachedCommentsJson, 
                    new com.fasterxml.jackson.core.type.TypeReference<List<PostComment>>() {}
                );
                
                // 处理图片URL，确保是完整URL
                processImageUrls(comments);
                
                return comments;
            } catch (Exception e) {
                System.err.println("从Redis获取评论列表缓存时出错: " + e.getMessage());
            }
        }
        
        // 缓存未命中，从数据库获取
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PostComment> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("post_id", postId);
        queryWrapper.orderByAsc("create_time");
        
        List<PostComment> comments = this.list(queryWrapper);
        
        // 处理图片URL，确保是完整URL
        processImageUrls(comments);
        
        // 将评论列表存入Redis缓存，过期时间为30-60分钟（随机）
        try {
            // 在存入Redis前，确保图片URL是完整URL
            for (PostComment comment : comments) {
                if (comment.getImageUrls() != null && !comment.getImageUrls().isEmpty()) {
                    try {
                        // 将JSON字符串转换为List
                        List<String> imageUrlList = objectMapper.readValue(
                            comment.getImageUrls(), 
                            new TypeReference<List<String>>() {}
                        );
                        
                        // 转换为完整URL
                        for (int i = 0; i < imageUrlList.size(); i++) {
                            String imageUrl = imageUrlList.get(i);
                            if (imageUrl != null && !imageUrl.startsWith("http")) {
                                imageUrlList.set(i, ossUtil.convertToFullUrl(imageUrl));
                            }
                        }
                        
                        // 更新imageUrls字段为包含完整URL的JSON字符串
                        String updatedImageUrlsJson = objectMapper.writeValueAsString(imageUrlList);
                        comment.setImageUrls(updatedImageUrlsJson);
                    } catch (Exception e) {
                        System.err.println("处理评论图片URL时出错: " + e.getMessage());
                    }
                }
            }
            
            String commentsJson = objectMapper.writeValueAsString(comments);
            Random random = new Random();
            int expireMinutes = 30 + random.nextInt(31); // 30-60之间的随机数
            redisTemplate.opsForValue().set(cacheKey, commentsJson, expireMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("将评论列表存入Redis缓存时出错: " + e.getMessage());
        }
        
        return comments;
    }
    
    /**
     * 处理评论中的图片URL，确保是完整URL
     * @param comments 评论列表
     */
    private void processImageUrls(List<PostComment> comments) {
        for (PostComment comment : comments) {
            if (comment.getImageUrls() != null && !comment.getImageUrls().isEmpty()) {
                try {
                    // 将JSON字符串转换为List
                    List<String> imageUrlList = objectMapper.readValue(
                        comment.getImageUrls(), 
                        new TypeReference<List<String>>() {}
                    );
                    
                    // 转换为完整URL
                    for (int i = 0; i < imageUrlList.size(); i++) {
                        String imageUrl = imageUrlList.get(i);
                        if (imageUrl != null && !imageUrl.startsWith("http")) {
                            imageUrlList.set(i, ossUtil.convertToFullUrl(imageUrl));
                        }
                    }
                    
                    // 更新imageUrls字段为包含完整URL的JSON字符串
                    String updatedImageUrlsJson = objectMapper.writeValueAsString(imageUrlList);
                    comment.setImageUrls(updatedImageUrlsJson);
                } catch (Exception e) {
                    System.err.println("处理评论图片URL时出错: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 清除指定帖子的评论缓存
     * @param postId 帖子ID
     */
    public void clearCommentsCache(String postId) {
        String cacheKey = COMMENT_LIST_PREFIX + postId;
        redisTemplate.delete(cacheKey);
    }
    
    @Override
    @Transactional
    public boolean toggleLike(String commentId, String userId, String username) {
        // 使用分布式锁防止并发问题
        String lockKey = "comment_like_lock:" + commentId;
        String lockValue = "locked";
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
        
        if (lockAcquired == null || !lockAcquired) {
            // 未能获取锁，直接返回
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        
        try {
            // 查询评论信息
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PostComment> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.eq("comment_id", commentId);
            
            PostComment comment = this.getOne(queryWrapper);
            
            if (comment == null) {
                throw new RuntimeException("评论不存在");
            }
            
            // 检查用户是否已经点赞过该评论
            String commentLikedUsersKey = COMMENT_LIKED_USERS_PREFIX + commentId;
            Boolean hasLiked = redisTemplate.opsForSet().isMember(commentLikedUsersKey, userId);
            
            // 获取评论点赞数的Redis键
            String commentLikeCountKey = COMMENT_LIKE_COUNT_PREFIX + commentId;
            
            if (hasLiked != null && hasLiked) {
                // 用户已点赞，取消点赞
                // 从Redis集合中移除用户ID
                redisTemplate.opsForSet().remove(commentLikedUsersKey, userId);
                // 减少点赞数
                redisTemplate.opsForValue().decrement(commentLikeCountKey);
                
                // 更新评论的点赞数
                comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            } else {
                // 用户未点赞，添加点赞
                // 将用户ID添加到Redis集合中
                redisTemplate.opsForSet().add(commentLikedUsersKey, userId);
                // 增加点赞数
                redisTemplate.opsForValue().increment(commentLikeCountKey);
                
                // 更新评论的点赞数
                comment.setLikeCount(comment.getLikeCount() + 1);
                
                // 设置过期时间，与评论缓存保持一致（30-60分钟随机）
                Random random = new Random();
                int expireMinutes = 30 + random.nextInt(31); // 30-60之间的随机数
                redisTemplate.expire(commentLikedUsersKey, expireMinutes, TimeUnit.MINUTES);
                redisTemplate.expire(commentLikeCountKey, expireMinutes, TimeUnit.MINUTES);
            }
            
            // 更新评论的更新时间
            comment.setUpdateTime(new Date());
            boolean updated = this.updateById(comment);
            if (!updated) {
                throw new RuntimeException("更新评论信息失败");
            }
            
            // 获取帖子ID用于清除缓存
            String postId = comment.getPostId();
            
            // 清除该帖子的评论缓存
            String cacheKey = COMMENT_LIST_PREFIX + postId;
            redisTemplate.delete(cacheKey);
            
            // 清除帖子详情缓存
            String postDetailCacheKey = "post_detail:" + postId;
            redisTemplate.delete(postDetailCacheKey);
            
            return !(hasLiked != null && hasLiked); // true表示已点赞，false表示已取消点赞
        } finally {
            // 释放分布式锁
            redisTemplate.delete(lockKey);
        }
    }
    
    @Override
    public boolean toggleLikeWithRabbitMQ(String commentId, String userId, String username) {
        // 检查用户点赞频率限制
        if (!commentLikeRabbitMQService.isUserAllowedToLike(userId)) {
            throw new RuntimeException("操作过于频繁，请稍后重试");
        }
        
        // 查询数据库确认真实的点赞状态（避免重复操作）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommentLike> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("comment_id", commentId).eq("user_id", userId);
        
        CommentLike existingLike = commentLikeServer.getOne(queryWrapper);
        
        boolean isActuallyLiked = existingLike != null;
        
        // 创建评论点赞消息
        com.lihuahua.hyperspace.models.entity.CommentLikeMessage message = 
            new com.lihuahua.hyperspace.models.entity.CommentLikeMessage();
        message.setAction(isActuallyLiked ? "unlike" : "like"); // 点赞或取消点赞操作
        message.setCommentId(commentId);
        message.setUserId(userId);
        message.setUsername(username);
        message.setCreateTime(new Date());
        
        // 发送到RabbitMQ
        commentLikeRabbitMQService.sendLikeMessage(message);
        
        return !isActuallyLiked; // true表示即将点赞，false表示即将取消点赞
    }
    
    /**
     * 获取评论的点赞数
     * @param commentId 评论ID
     * @return 点赞数
     */
    public int getCommentLikeCount(String commentId) {
        String commentLikeCountKey = COMMENT_LIKE_COUNT_PREFIX + commentId;
        String likeCountStr = redisTemplate.opsForValue().get(commentLikeCountKey);
        
        if (likeCountStr != null) {
            try {
                return Integer.parseInt(likeCountStr);
            } catch (NumberFormatException e) {
                // 如果解析失败，从数据库获取并更新缓存
            }
        }
        
        // 缓存未命中，从数据库获取
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PostComment> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("comment_id", commentId);
        
        PostComment comment = this.getOne(queryWrapper);
        if (comment != null) {
            int likeCount = comment.getLikeCount();
            // 将点赞数存入Redis缓存，过期时间为30-60分钟（随机）
            Random random = new Random();
            int expireMinutes = 30 + random.nextInt(31); // 30-60之间的随机数
            redisTemplate.opsForValue().set(commentLikeCountKey, String.valueOf(likeCount), expireMinutes, TimeUnit.MINUTES);
            return likeCount;
        }
        
        return 0;
    }
    
    /**
     * 检查用户是否已点赞某条评论
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 是否已点赞
     */
    public boolean isUserLikedComment(String commentId, String userId) {
        String commentLikedUsersKey = COMMENT_LIKED_USERS_PREFIX + commentId;
        Boolean isLiked = redisTemplate.opsForSet().isMember(commentLikedUsersKey, userId);
        
        // 如果Redis中没有记录，则查询数据库
        if (isLiked == null) {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommentLike> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.eq("comment_id", commentId).eq("user_id", userId);
            
            CommentLike commentLike = commentLikeServer.getOne(queryWrapper);
            isLiked = commentLike != null;
            
            // 将结果缓存到Redis中，过期时间为30-60分钟（随机）
            if (isLiked) {
                Random random = new Random();
                int expireMinutes = 30 + random.nextInt(31); // 30-60之间的随机数
                redisTemplate.opsForSet().add(commentLikedUsersKey, userId);
                redisTemplate.expire(commentLikedUsersKey, expireMinutes, TimeUnit.MINUTES);
            }
        }
        
        return isLiked != null && isLiked;
    }
    
    /**
     * 预热用户对评论的点赞数据到Redis缓存
     * @param commentId 评论ID
     * @param userId 用户ID
     */
    public void warmupUserCommentLikeData(String commentId, String userId) {
        warmupUserCommentLikeData(commentId, userId, null, null);
    }
    
    /**
     * 预热用户对评论的点赞数据到Redis缓存
     * @param commentId 评论ID
     * @param userId 用户ID
     * @param postLikedUsersKey 帖子点赞数据Redis键（可选）
     * @param postId 帖子ID（可选）
     */
    public void warmupUserCommentLikeData(String commentId, String userId, String postLikedUsersKey, String postId) {
        if (userId == null || userId.isEmpty()) {
            System.out.println("用户ID为空，跳过预热");
            return;
        }
        
        try {
            // 预热评论点赞数据
            String commentLikedUsersKey = COMMENT_LIKED_USERS_PREFIX + commentId;
            
            // 检查Redis中是否已经有该用户的点赞状态
            Boolean userLikedComment = redisTemplate.opsForSet().isMember(commentLikedUsersKey, userId);
            
            // 当userLikedComment为null时表示Redis中没有这个key
            if (userLikedComment == null|| !userLikedComment) {
                // Redis中没有该用户的点赞状态，从数据库查询
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommentLike> queryWrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("comment_id", commentId);
                queryWrapper.eq("user_id", userId);
                
                CommentLike existingLike = commentLikeServer.getOne(queryWrapper);
                
                if (existingLike != null) {
                    // 用户已点赞该评论
                    redisTemplate.opsForSet().add(commentLikedUsersKey, userId);
                }
                
                // 设置过期时间
                redisTemplate.expire(commentLikedUsersKey, 60, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            System.err.println("预热用户评论点赞数据失败，用户ID: " + userId + ", 评论ID: " + commentId + ", 错误: " + e.getMessage());
        }
    }
    
    /**
     * 预热用户对帖子的点赞数据到Redis缓存
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 帖子点赞数据Redis键
     */
    public String warmupUserPostLikeData(String postId, String userId) {
        if (userId == null || userId.isEmpty()) {
            System.out.println("用户ID为空，跳过帖子点赞预热");
            return null;
        }
        
        try {
            // 预热帖子点赞数据
            String postLikedUsersKey = "post_liked_users:" + postId;
            Boolean userLikedPost = redisTemplate.opsForSet().isMember(postLikedUsersKey, userId);
            
            if (userLikedPost == null|| !userLikedPost) {
                // Redis中没有该用户的点赞状态，从数据库查询
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.lihuahua.hyperspace.models.entity.PostLike> queryWrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("post_id", postId);
                queryWrapper.eq("user_id", userId);
                
                com.lihuahua.hyperspace.models.entity.PostLike existingLike = 
                    postLikeServer.getOne(queryWrapper);
                
                if (existingLike != null) {
                    // 用户已点赞该帖子
                    redisTemplate.opsForSet().add(postLikedUsersKey, userId);
                }
                
                // 设置过期时间
                redisTemplate.expire(postLikedUsersKey, 60, TimeUnit.MINUTES);
            }
            
            return postLikedUsersKey;
        } catch (Exception e) {
            System.err.println("预热用户帖子点赞数据失败，用户ID: " + userId + ", 帖子ID: " + postId + ", 错误: " + e.getMessage());
            return null;
        }
    }
}