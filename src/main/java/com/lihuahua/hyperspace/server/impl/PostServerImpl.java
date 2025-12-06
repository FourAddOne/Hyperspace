package com.lihuahua.hyperspace.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.mapper.PostMapper;
import com.lihuahua.hyperspace.models.entity.Post;
import com.lihuahua.hyperspace.models.entity.PostLike;
import com.lihuahua.hyperspace.models.entity.PostLikeMessage;
import com.lihuahua.hyperspace.models.entity.PostMedia;
import com.lihuahua.hyperspace.server.PostLikeServer;
import com.lihuahua.hyperspace.server.PostMediaServer;
import com.lihuahua.hyperspace.server.PostServer;
import com.lihuahua.hyperspace.service.PostLikeMessageService;
import com.lihuahua.hyperspace.service.PostLikeRabbitMQService;
import com.lihuahua.hyperspace.utils.OssUtil;
import com.lihuahua.hyperspace.utils.SnowflakeIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lihuahua.hyperspace.models.entity.PostComment;
import com.lihuahua.hyperspace.server.PostCommentServer;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

@Service
public class PostServerImpl extends ServiceImpl<PostMapper, Post> implements PostServer {
    
    private static final Logger logger = LoggerFactory.getLogger(PostServerImpl.class);
    
    @Autowired
    private PostLikeServer postLikeServer;
    
    @Autowired
    private PostMediaServer postMediaServer;
    
    @Autowired
    private PostLikeMessageService postLikeMessageService;
    
    @Autowired
    private PostLikeRabbitMQService postLikeRabbitMQService;
    
    @Autowired
    private PostCommentServer postCommentServer;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private OssUtil ossUtil;
    
    // ObjectMapper用于序列化和反序列化JSON
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 分布式锁前缀
    private static final String LIKE_LOCK_PREFIX = "post_like_lock:";
    
    // 点赞计数缓存前缀
    private static final String LIKE_COUNT_PREFIX = "post_like_count:";
    
    // 帖子详情缓存前缀
    private static final String POST_DETAIL_PREFIX = "post_detail:";
    
    // 帖子列表缓存前缀
    private static final String POST_LIST_PREFIX = "post_list:";
    
    // 帖子创建幂等性校验前缀
    private static final String POST_CREATE_IDEMPOTENT_PREFIX = "post_create_idempotent:";
    
    // 帖子点赞用户集合前缀
    private static final String POST_LIKED_USERS_PREFIX = "post_liked_users:";
    
    // 帖子浏览用户集合前缀
    private static final String POST_VIEW_USERS_PREFIX = "post_view_users:";
    
    @Override
    public Map<String, Object> createPost(Post post) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 生成请求指纹，用于幂等性校验
            String requestId = generateRequestId(post);
            String idempotentKey = POST_CREATE_IDEMPOTENT_PREFIX + requestId;
            
            // 检查是否已经处理过相同的请求
            String existingPostId = redisTemplate.opsForValue().get(idempotentKey);
            if (existingPostId != null) {
                // 如果已经存在，直接返回之前的结果
                Post existingPost = this.getByPostId(existingPostId);
                if (existingPost != null) {
                    response.put("code", 200);
                    response.put("message", "发布成功");
                    response.put("data", existingPost);
                    return response;
                }
            }
            
            System.out.println("接收到创建帖子请求: " + post);
            // 设置帖子ID
            post.setPostId("post_" + SnowflakeIdUtil.nextId());
            post.setCreateTime(new Date());
            post.setUpdateTime(new Date());
            
            System.out.println("准备保存的帖子数据: " + post);
            
            // 保存帖子
            boolean saved = this.save(post);
            
            System.out.println("帖子保存结果: " + saved);
            
            if (saved) {
                // 将请求指纹和帖子ID存入Redis，设置5分钟过期时间
                redisTemplate.opsForValue().set(idempotentKey, post.getPostId(), 5, TimeUnit.MINUTES);
                
                response.put("code", 200);
                response.put("message", "发布成功");
                response.put("data", post);
            } else {
                response.put("code", 500);
                response.put("message", "发布失败");
            }
        } catch (Exception e) {
            System.err.println("创建帖子时发生异常: " + e.getMessage());
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        
        return response;
    }
    
    // 生成请求指纹
    private String generateRequestId(Post post) {
        // 使用用户ID、标题、内容生成指纹
        StringBuilder sb = new StringBuilder();
        sb.append(post.getUserId() != null ? post.getUserId() : "");
        sb.append("_");
        sb.append(post.getTitle() != null ? post.getTitle() : "");
        sb.append("_");
        sb.append(post.getContent() != null ? post.getContent().hashCode() : "");
        sb.append("_");
        sb.append(post.getType() != null ? post.getType() : "");
        return sb.toString();
    }
    
    // 根据postId获取帖子
    private Post getByPostId(String postId) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("post_id", postId);
        return this.getOne(queryWrapper);
    }
    
    @Override
    public Map<String, Object> getPosts(Integer page, Integer size, String category, String type) {
        // 生成缓存键
        String cacheKey = POST_LIST_PREFIX + page + ":" + size;
        if (category != null && !category.isEmpty()) {
            cacheKey += ":category=" + category;
        }
        if (type != null && !type.isEmpty()) {
            cacheKey += ":type=" + type;
        }
        
        try {
            // 尝试从Redis缓存中获取帖子列表
            String cachedPostsJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedPostsJson != null) {
                // 缓存命中，直接返回缓存数据
                System.out.println("从Redis缓存中获取帖子列表，cacheKey: " + cacheKey);
                List<Post> cachedPosts = objectMapper.readValue(
                    cachedPostsJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Post.class)
                );
                
                Map<String, Object> response = new HashMap<>();
                response.put("code", 200);
                response.put("data", cachedPosts);
                response.put("source", "redis"); // 标识数据来源于Redis
                return response;
            }
        } catch (Exception e) {
            System.err.println("从Redis获取帖子列表缓存时出错: " + e.getMessage());
        }
        
        Page<Post> postPage = new Page<>(page, size);
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "published");
        
        // 按分类筛选
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq("category", category);
        }
        
        // 按类型筛选
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq("type", type);
        }
        
        queryWrapper.orderByDesc("create_time");

        Page<Post> result = this.page(postPage, queryWrapper);

        // 为每个帖子加载媒体资源并转换相对路径为完整URL
        for (Post post : result.getRecords()) {
            // 转换头像URL为完整URL（无论什么情况都要确保是完整URL）
            if (post.getAvatarUrl() != null && !post.getAvatarUrl().isEmpty()) {
                if (!post.getAvatarUrl().startsWith("http")) {
                    post.setAvatarUrl(ossUtil.convertToFullUrl(post.getAvatarUrl()));
                }
            } else {
                // 如果头像为空，设置默认头像
                post.setAvatarUrl("/src/assets/logo.svg");
            }
            
            // 转换封面图片URL为完整URL
            if (post.getCoverUrl() != null && !post.getCoverUrl().isEmpty()) {
                if (!post.getCoverUrl().startsWith("http")) {
                    post.setCoverUrl(ossUtil.convertToFullUrl(post.getCoverUrl()));
                }
            }
            
            // 从Redis获取最新的点赞数
            String likeCountStr = redisTemplate.opsForValue().get(LIKE_COUNT_PREFIX + post.getPostId());
            if (likeCountStr != null) {
                try {
                    int likeCount = Integer.parseInt(likeCountStr);
                    post.setLikeCount(likeCount);
                } catch (NumberFormatException e) {
                    System.err.println("解析点赞数失败: " + likeCountStr);
                }
            }
            
            QueryWrapper<PostMedia> mediaQueryWrapper = new QueryWrapper<>();
            mediaQueryWrapper.eq("post_id", post.getPostId());
            List<PostMedia> mediaList = postMediaServer.list(mediaQueryWrapper);
            post.setMediaList(mediaList);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("pages", result.getPages());
        response.put("source", "mysql"); // 标识数据来源于MySQL
        
        // 将帖子列表存入Redis缓存，过期时间为30-60分钟（随机）
        try {
            Random random = new Random();
            int expireMinutes = 30 + random.nextInt(31); // 30-60之间的随机数
            String postsJson = objectMapper.writeValueAsString(result.getRecords());
            redisTemplate.opsForValue().set(cacheKey, postsJson, expireMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("将帖子列表存入Redis缓存时出错: " + e.getMessage());
        }

        return response;
    }
    
    @Override
    public Map<String, Object> getPostDetail(String postId, String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 先增加浏览计数（内部会处理去重逻辑）
            incrementViewCount(postId, userId);
            
            // 预热用户对该帖子及其评论的点赞数据
            warmupUserLikeData(postId, userId);
            
            // 然后尝试从Redis缓存中获取帖子详情
            String cacheKey = POST_DETAIL_PREFIX + postId;
            String cachedPostJson = redisTemplate.opsForValue().get(cacheKey);
            
            Post post;
            if (cachedPostJson != null) {
                // 缓存命中，直接返回缓存数据
                post = objectMapper.readValue(cachedPostJson, Post.class);
            } else {
                // 缓存未命中，从数据库获取帖子信息
                QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("post_id", postId);
                post = this.getOne(queryWrapper);
                
                if (post != null) {
                    // 转换头像URL为完整URL（无论什么情况都要确保是完整URL）
                    if (post.getAvatarUrl() != null && !post.getAvatarUrl().isEmpty()) {
                        if (!post.getAvatarUrl().startsWith("http")) {
                            post.setAvatarUrl(ossUtil.convertToFullUrl(post.getAvatarUrl()));
                        }
                    } else {
                        // 如果头像为空，设置默认头像
                        post.setAvatarUrl("/src/assets/logo.svg");
                    }
                    
                    // 转换封面图片URL为完整URL
                    if (post.getCoverUrl() != null && !post.getCoverUrl().isEmpty()) {
                        if (!post.getCoverUrl().startsWith("http")) {
                            post.setCoverUrl(ossUtil.convertToFullUrl(post.getCoverUrl()));
                        }
                    }
                    
                    // 获取媒体资源
                    QueryWrapper<PostMedia> mediaQueryWrapper = new QueryWrapper<>();
                    mediaQueryWrapper.eq("post_id", postId);
                    List<PostMedia> mediaList = postMediaServer.list(mediaQueryWrapper);
                    post.setMediaList(mediaList);
                    
                    // 将帖子详情存入Redis缓存，过期时间为30-60分钟（随机）
                    Random random = new Random();
                    int expireMinutes = 30 + random.nextInt(31); // 30-60之间的随机数
                    String postJson = objectMapper.writeValueAsString(post);
                    redisTemplate.opsForValue().set(cacheKey, postJson, expireMinutes, TimeUnit.MINUTES);
                }
            }
            
            if (post != null) {
                // 如果提供了用户ID，检查该用户是否已点赞此帖子
                if (userId != null && !userId.isEmpty()) {
                    // 使用Redis Set结构检查用户是否已点赞
                    String postLikedUsersKey = POST_LIKED_USERS_PREFIX + postId;
                    Boolean isLiked = redisTemplate.opsForSet().isMember(postLikedUsersKey, userId);
                    
                    // 如果Redis中有缓存数据，直接使用
                    if (isLiked != null) {
                        post.setIsLiked(isLiked);
                    } else {
                        // Redis中没有缓存数据，查询数据库并将结果缓存到Redis
                        try {
                            PostLike existingLike = postLikeServer.getOne(
                                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PostLike>()
                                    .eq("post_id", postId)
                                    .eq("user_id", userId)
                            );
                            
                            boolean isActuallyLiked = existingLike != null;
                            post.setIsLiked(isActuallyLiked);
                            
                            // 将用户点赞状态缓存到Redis中
                            if (isActuallyLiked) {
                                redisTemplate.opsForSet().add(postLikedUsersKey, userId);
                            } else {
                                // 确保未点赞的用户不在集合中
                                redisTemplate.opsForSet().remove(postLikedUsersKey, userId);
                            }
                            
                            // 设置过期时间与帖子详情缓存保持一致
                            redisTemplate.expire(postLikedUsersKey, 60, TimeUnit.MINUTES);
                        } catch (Exception dbException) {
                            // 数据库查询出错时，默认设置为未点赞
                            post.setIsLiked(false);
                            System.err.println("查询用户点赞状态时出错: " + dbException.getMessage());
                        }
                    }
                } else {
                    // 用户未登录，设置默认未点赞状态
                    post.setIsLiked(false);
                }
                
                response.put("code", 200);
                response.put("data", post);
                response.put("source", cachedPostJson != null ? "redis" : "mysql");
            } else {
                response.put("code", 404);
                response.put("message", "帖子不存在");
                response.put("source", "none");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            response.put("source", "error");
        }
        
        return response;
    }
    
    /**
     * 增加帖子浏览计数
     * @param postId 帖子ID
     * @param userId 用户ID
     */
    private void incrementViewCount(String postId, String userId) {
        try {
            String postViewUsersKey = POST_VIEW_USERS_PREFIX + postId;
            
            // 检查用户今天是否已经浏览过该帖子
            Boolean hasViewed = redisTemplate.opsForSet().isMember(postViewUsersKey, userId);
            
            // 如果用户今天还没浏览过该帖子，则增加浏览计数
            if (hasViewed == null || !hasViewed) {
                // 更新数据库中的浏览计数
                Post post = this.getOne(new QueryWrapper<Post>().eq("post_id", postId));
                if (post != null) {
                    post.setViewCount(post.getViewCount() + 1);
                    post.setUpdateTime(new Date());
                    this.updateById(post);
                    
                    // 将用户ID添加到已浏览用户集合中，过期时间为24小时
                    redisTemplate.opsForSet().add(postViewUsersKey, userId);
                    redisTemplate.expire(postViewUsersKey, 24, TimeUnit.HOURS);
                    
                    // 清除帖子详情缓存，因为浏览数发生了变化
                    redisTemplate.delete(POST_DETAIL_PREFIX + postId);
                    
                    // 清除帖子列表缓存，因为浏览数发生了变化
                    clearPostListCache();
                }
            }
        } catch (Exception e) {
            // 浏览计数增加失败不应影响主要功能，所以只打印错误日志
            System.err.println("增加帖子浏览计数时出错: " + e.getMessage());
        }
    }

    /**
     * 点赞/取消点赞操作 - 使用延迟双删策略保证数据一致性
     * @param postId 帖子ID
     * @param userId 用户ID
     * @param username 用户名
     * @return true表示已点赞，false表示已取消点赞
     */
    @Override
    @Transactional
    public boolean toggleLike(String postId, String userId, String username) {
        // 使用分布式锁防止并发问题
        String lockKey = "post_like_lock:" + postId;
        String lockValue = "locked";
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
        
        if (lockAcquired == null || !lockAcquired) {
            // 未能获取锁，直接返回
            throw new RuntimeException("系统繁忙，请稍后重试");
        }
        
        try {
            // 查询帖子信息
            QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("post_id", postId);
            
            Post post = this.getOne(queryWrapper);
            
            if (post == null) {
                throw new RuntimeException("帖子不存在");
            }
            
            // 检查用户是否已经点赞过该帖子
            String postLikedUsersKey = POST_LIKED_USERS_PREFIX + postId;
            Boolean hasLiked = redisTemplate.opsForSet().isMember(postLikedUsersKey, userId);
            
            // 获取帖子点赞数的Redis键
            String likeCountKey = LIKE_COUNT_PREFIX + postId;
            
            // 处理取消点赞的情况
            if (hasLiked != null && hasLiked) {
                // 用户已点赞，取消点赞
                // 从数据库中删除点赞记录
                QueryWrapper<PostLike> likeQueryWrapper = new QueryWrapper<>();
                likeQueryWrapper.eq("post_id", postId).eq("user_id", userId);
                boolean removed = postLikeServer.remove(likeQueryWrapper);
                if (!removed) {
                    throw new RuntimeException("删除点赞记录失败");
                }
                
                // 从Redis集合中移除用户ID
                redisTemplate.opsForSet().remove(postLikedUsersKey, userId);
                // 减少点赞数
                redisTemplate.opsForValue().decrement(likeCountKey);
                
                post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            } else {
                // 用户未点赞，添加点赞
                
                // 先检查数据库中是否真的不存在点赞记录（防止重复）
                PostLike existingLike = postLikeServer.getOne(
                    new QueryWrapper<PostLike>()
                        .eq("post_id", postId)
                        .eq("user_id", userId)
                );
                
                // 只有当数据库中确实不存在点赞记录时才插入新记录
                if (existingLike == null) {
                    PostLike like = new PostLike();
                    like.setLikeId("like_" + SnowflakeIdUtil.nextId());
                    like.setPostId(postId);
                    like.setUserId(userId);
                    like.setUsername(username);
                    like.setCreateTime(new Date());
                    boolean saved = postLikeServer.save(like);
                    if (!saved) {
                        throw new RuntimeException("保存点赞记录失败");
                    }
                }
                
                // 将用户ID添加到Redis集合中
                redisTemplate.opsForSet().add(postLikedUsersKey, userId);
                // 增加点赞数
                redisTemplate.opsForValue().increment(likeCountKey);
                
                post.setLikeCount(post.getLikeCount() + 1);
            }
            
            // 更新帖子的更新时间
            post.setUpdateTime(new Date());
            boolean updated = this.updateById(post);
            if (!updated) {
                throw new RuntimeException("更新帖子信息失败");
            }
            
            // 更新帖子详情缓存中的点赞数
            updatePostDetailCacheLikeCount(postId, post.getLikeCount());
            
            // 延迟双删第一步：立即删除帖子详情缓存
            String postDetailCacheKey = POST_DETAIL_PREFIX + postId;
            redisTemplate.delete(postDetailCacheKey);
            
            // 异步延迟删除帖子列表缓存
            clearPostListCacheAsync();
            
            // 延迟双删第二步：延迟一段时间后再删除一次缓存
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(1000); // 延迟1秒
                    redisTemplate.delete(postDetailCacheKey);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            // 确保Redis中的用户点赞状态与数据库一致
            Boolean finalHasLiked = redisTemplate.opsForSet().isMember(postLikedUsersKey, userId);
            return finalHasLiked == null ? false : finalHasLiked; // true表示已点赞，false表示未点赞
        } finally {
            // 释放分布式锁
            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * 更新帖子详情缓存中的点赞数
     * @param postId 帖子ID
     * @param likeCount 新的点赞数
     */
    private void updatePostDetailCacheLikeCount(String postId, int likeCount) {
        String postDetailCacheKey = POST_DETAIL_PREFIX + postId;
        String postDetailJson = redisTemplate.opsForValue().get(postDetailCacheKey);
        
        if (postDetailJson != null) {
            try {
                // 解析缓存中的帖子对象
                Post post = objectMapper.readValue(postDetailJson, Post.class);
                // 更新点赞数
                post.setLikeCount(likeCount);
                
                // 确保头像和封面URL是完整URL
                if (post.getAvatarUrl() != null && !post.getAvatarUrl().isEmpty()) {
                    if (!post.getAvatarUrl().startsWith("http")) {
                        post.setAvatarUrl(ossUtil.convertToFullUrl(post.getAvatarUrl()));
                    }
                } else {
                    // 如果头像为空，设置默认头像
                    post.setAvatarUrl("/src/assets/logo.svg");
                }
                
                // 转换封面图片URL为完整URL
                if (post.getCoverUrl() != null && !post.getCoverUrl().isEmpty()) {
                    if (!post.getCoverUrl().startsWith("http")) {
                        post.setCoverUrl(ossUtil.convertToFullUrl(post.getCoverUrl()));
                    }
                }
                
                // 重新序列化并更新缓存
                String updatedPostDetailJson = objectMapper.writeValueAsString(post);
                redisTemplate.opsForValue().set(postDetailCacheKey, updatedPostDetailJson, 30, TimeUnit.MINUTES);
            } catch (Exception e) {
                // 如果更新失败，则删除缓存，下次访问时重新加载
                redisTemplate.delete(postDetailCacheKey);
            }
        }
    }
    
    /**
     * 清除帖子列表缓存
     * 由于Redis不支持通配符删除，我们需要遍历可能的键并删除它们
     */
    private void clearPostListCache() {
        // 删除常见的帖子列表缓存键
        // 注意：Redis不支持批量删除通配符匹配的键，所以我们需要尽可能多地删除常见键
        
        // 删除第1-10页的常见组合
        for (int page = 1; page <= 10; page++) {
            for (int size = 10; size <= 30; size += 10) {
                String key = POST_LIST_PREFIX + page + ":" + size;
                redisTemplate.delete(key);
                
                // 删除带分类的键
                redisTemplate.delete(key + ":category=video");
                redisTemplate.delete(key + ":category=article");
                redisTemplate.delete(key + ":category=guide");
                
                // 删除带类型的键
                redisTemplate.delete(key + ":type=video");
                redisTemplate.delete(key + ":type=article");
                redisTemplate.delete(key + ":type=guide");
            }
        }
    }
    
    /**
     * 异步清除帖子列表缓存
     */
    private void clearPostListCacheAsync() {
        CompletableFuture.runAsync(() -> {
            clearPostListCache();
        });
    }

    @Override
    public boolean toggleLikeWithRabbitMQ(String postId, String userId, String username) {
        // 检查用户点赞频率限制
        if (!postLikeRabbitMQService.isUserAllowedToLike(userId)) {
            throw new RuntimeException("操作过于频繁，请稍后重试");
        }
        
        // 查询数据库确认真实的点赞状态（避免重复操作）
        PostLike existingLike = postLikeServer.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PostLike>()
                .eq("post_id", postId)
                .eq("user_id", userId)
        );
        
        boolean isActuallyLiked = existingLike != null;
        
        // 创建点赞消息
        PostLikeMessage message = new PostLikeMessage();
        message.setAction(isActuallyLiked ? "unlike" : "like"); // 点赞或取消点赞操作
        message.setPostId(postId);
        message.setUserId(userId);
        message.setUsername(username);
        message.setCreateTime(new Date());
        
        // 发送到RabbitMQ
        postLikeRabbitMQService.sendLikeMessage(message);
        
        return !isActuallyLiked; // true表示即将点赞，false表示即将取消点赞
    }
    
    /**
     * 预热用户对帖子及其评论的点赞数据到Redis缓存
     * @param postId 帖子ID
     * @param userId 用户ID
     */
    public void warmupUserLikeData(String postId, String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        try {
            // 预热帖子点赞数据
            String postLikedUsersKey = POST_LIKED_USERS_PREFIX + postId;
            
            // 检查Redis中是否已经有该用户的点赞状态
            Boolean userLikedPost = redisTemplate.opsForSet().isMember(postLikedUsersKey, userId);
            
            // 当userLikedPost为null时表示Redis中没有这个key
            if (userLikedPost == null|| !userLikedPost) {
                // Redis中没有该用户的点赞状态，从数据库查询
                PostLike existingLike = postLikeServer.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PostLike>()
                        .eq("post_id", postId)
                        .eq("user_id", userId)
                );
                
                if (existingLike != null) {
                    // 用户已点赞该帖子
                    redisTemplate.opsForSet().add(postLikedUsersKey, userId);
                }
                
                // 设置过期时间
                redisTemplate.expire(postLikedUsersKey, 60, TimeUnit.MINUTES);
            }
            
            // 预热该帖子下所有评论的用户点赞数据
            List<PostComment> comments = postCommentServer.getCommentsByPostId(postId);
            postCommentServer.warmupUserPostLikeData(postId, userId);
            for (PostComment comment : comments) {
                postCommentServer.warmupUserCommentLikeData(comment.getCommentId(), userId);
            }
        } catch (Exception e) {
            logger.error("预热用户帖子及评论点赞数据失败，用户ID: {}, 帖子ID: {}, 错误: {}", userId, postId, e.getMessage(), e);
        }
    }

}