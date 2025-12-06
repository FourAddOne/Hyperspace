package com.lihuahua.hyperspace.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.models.entity.Post;
import com.lihuahua.hyperspace.models.entity.PostComment;
import com.lihuahua.hyperspace.models.entity.PostLike;
import com.lihuahua.hyperspace.models.entity.CommentLikeMessage;
import com.lihuahua.hyperspace.server.PostCommentServer;
import com.lihuahua.hyperspace.server.PostServer;
import com.lihuahua.hyperspace.service.CommentLikeRabbitMQService;
import com.lihuahua.hyperspace.utils.SnowflakeIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 帖子控制器
 */
@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostServer postServer;

    @Autowired
    private PostCommentServer postCommentServer;
    
    @Autowired
    private CommentLikeRabbitMQService commentLikeRabbitMQService;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    // 评论列表缓存前缀
    private static final String COMMENT_LIST_PREFIX = "post_comments:";

    /**
     * 创建帖子
     *
     * @param post 帖子信息
     * @return 创建结果
     */
    @PostMapping("/create")
    public Map<String, Object> createPost(@RequestBody Post post) {
        return postServer.createPost(post);
    }

    /**
     * 获取帖子列表（分页）
     *
     * @param page 页码
     * @param size 每页数量
     * @param category 分类
     * @param type 类型
     * @return 帖子列表
     */
    @GetMapping
    public Map<String, Object> getPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        return postServer.getPosts(page, size, category, type);
    }

    /**
     * 获取帖子详情
     *
     * @param postId 帖子ID
     * @return 帖子详情
     */
    @GetMapping("/detail/{postId}")
    public Map<String, Object> getPostDetail(@PathVariable String postId, HttpServletRequest request) {
        // 从请求中获取当前用户信息
        String userId = (String) request.getAttribute("userId");
        return postServer.getPostDetail(postId, userId);
    }

    /**
     * 获取帖子评论列表
     *
     * @param postId 帖子ID
     * @return 评论列表
     */
    @GetMapping("/comments/{postId}")
    public Map<String, Object> getPostComments(@PathVariable String postId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 从请求中获取当前用户信息
            String userId = (String) request.getAttribute("userId");
            
            // 尝试从Redis缓存中获取评论列表
            String cacheKey = COMMENT_LIST_PREFIX + postId;
            String cachedCommentsJson = redisTemplate.opsForValue().get(cacheKey);
            
            List<PostComment> comments;
            if (cachedCommentsJson != null) {
                // 缓存命中，直接返回缓存数据
                System.out.println("从Redis缓存中获取评论列表，cacheKey: " + cacheKey);
                comments = objectMapper.readValue(cachedCommentsJson, new TypeReference<List<PostComment>>() {});
            } else {
                // 缓存未命中，从数据库获取
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PostComment> queryWrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("post_id", postId);
                queryWrapper.orderByAsc("create_time");
                
                comments = postCommentServer.list(queryWrapper);
                
                // 将评论列表存入Redis缓存，过期时间为30-60分钟（随机）
                String commentsJson = objectMapper.writeValueAsString(comments);
                Random random = new Random();
                int expireMinutes = 30 + random.nextInt(31); // 30-60之间的随机数
                redisTemplate.opsForValue().set(cacheKey, commentsJson, expireMinutes, TimeUnit.MINUTES);
                System.out.println("评论列表已存入Redis缓存，cacheKey: " + cacheKey + "，过期时间: " + expireMinutes + "分钟");
            }
            
            // 处理用户点赞状态和点赞数
            if (userId != null && !userId.isEmpty()) {
                for (PostComment comment : comments) {
                    // 获取评论点赞数
                    comment.setLikeCount(postCommentServer.getCommentLikeCount(comment.getCommentId().toString()));
                    
                    // 检查当前用户是否已点赞该评论
                    boolean isLiked = postCommentServer.isUserLikedComment(comment.getCommentId().toString(), userId);
                    comment.setIsLiked(isLiked);
                }
            }
            
            response.put("code", 200);
            response.put("data", comments);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 创建评论
     *
     * @param postId 帖子ID
     * @param comment 评论内容
     * @return 评论结果
     */
    @PostMapping("/comment/create/{postId}")
    @Transactional
    public Map<String, Object> createComment(
            @PathVariable String postId, 
            @RequestBody PostComment comment) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 设置评论ID和帖子ID
            comment.setCommentId("comment_" + SnowflakeIdUtil.nextId());
            comment.setPostId(postId);
            comment.setCreateTime(new Date());
            comment.setUpdateTime(new Date());
            
            // 保存评论
            boolean saved = postCommentServer.save(comment);
            
            if (saved) {
                // 更新帖子的评论数
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Post> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("post_id", postId);
                Post post = postServer.getOne(queryWrapper);
                if (post != null) {
                    post.setCommentCount(post.getCommentCount() + 1);
                    post.setUpdateTime(new Date());
                    postServer.updateById(post);
                }
                
                // 清除该帖子的评论缓存
                String cacheKey = COMMENT_LIST_PREFIX + postId;
                redisTemplate.delete(cacheKey);
                
                // 清除帖子详情缓存
                String postDetailCacheKey = "post_detail:" + postId;
                redisTemplate.delete(postDetailCacheKey);
                
                // 清除帖子列表缓存
                clearPostListCache();
                
                response.put("code", 200);
                response.put("message", "评论成功");
                response.put("data", comment);
            } else {
                response.put("code", 500);
                response.put("message", "评论失败");
            }
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
            throw e; // 传播异常以触发事务回滚
        }
        
        return response;
    }
    
    /**
     * 点赞评论（使用RabbitMQ异步处理）
     *
     * @param commentId 评论ID
     * @param like 点赞信息
     * @return 点赞结果
     */
    @PostMapping("/comment/like/{commentId}")
    public Map<String, Object> likeComment(
            @PathVariable String commentId,
            @RequestBody Map<String, String> like) {
    
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userId = like.get("userId");
            String username = like.get("username");
            
            // 使用基于RabbitMQ的异步方式处理点赞逻辑
            boolean result = postCommentServer.toggleLikeWithRabbitMQ(commentId, userId, username);
            
            response.put("code", 200);
            response.put("message", "请求已提交");
            response.put("data", result);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "服务器内部错误: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 回复评论
     *
     * @param commentId 被回复的评论ID
     * @param comment 回复内容
     * @return 回复结果
     */
    @PostMapping("/comment/reply/{commentId}")
    @Transactional
    public Map<String, Object> replyComment(
            @PathVariable String commentId,
            @RequestBody PostComment comment) {
    
    Map<String, Object> response = new HashMap<>();
    
    try {
        // 获取被回复的评论
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PostComment> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("comment_id", commentId);
        
        PostComment parentComment = postCommentServer.getOne(queryWrapper);
        
        if (parentComment != null) {
            // 设置回复评论的属性
            comment.setCommentId("comment_" + SnowflakeIdUtil.nextId());
            comment.setPostId(parentComment.getPostId());
            comment.setParentId(commentId);
            comment.setReplyToUserId(parentComment.getUserId());
            comment.setReplyToUsername(parentComment.getUsername());
            comment.setCreateTime(new Date());
            comment.setUpdateTime(new Date());
            
            // 保存回复评论
            boolean saved = postCommentServer.save(comment);
            
            if (saved) {
                // 更新帖子的评论数
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Post> postQueryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                postQueryWrapper.eq("post_id", parentComment.getPostId());
                Post post = postServer.getOne(postQueryWrapper);
                if (post != null) {
                    post.setCommentCount(post.getCommentCount() + 1);
                    post.setUpdateTime(new Date());
                    postServer.updateById(post);
                }
                
                // 清除该帖子的评论缓存
                String cacheKey = COMMENT_LIST_PREFIX + parentComment.getPostId();
                redisTemplate.delete(cacheKey);
                
                // 清除帖子详情缓存
                String postDetailCacheKey = "post_detail:" + parentComment.getPostId();
                redisTemplate.delete(postDetailCacheKey);
                
                // 清除帖子列表缓存
                clearPostListCache();
                
                response.put("code", 200);
                response.put("message", "回复成功");
                response.put("data", comment);
            } else {
                response.put("code", 500);
                response.put("message", "回复失败");
            }
        } else {
            response.put("code", 404);
            response.put("message", "被回复的评论不存在");
        }
    } catch (Exception e) {
        response.put("code", 500);
        response.put("message", "服务器内部错误: " + e.getMessage());
        throw e; // 传播异常以触发事务回滚
    }
    
    return response;
}
    
    /**
     * 点赞帖子
     *
     * @param postId 帖子ID
     * @param like 点赞信息
     * @return 点赞结果
     */
    @PostMapping("/like/{postId}")
    public Map<String, Object> likePost(
            @PathVariable String postId,
            @RequestBody PostLike like) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 使用基于RabbitMQ的异步方式处理点赞逻辑
            boolean result = postServer.toggleLikeWithRabbitMQ(postId, like.getUserId(), like.getUsername());
            
            response.put("code", 200);
            response.put("message", "请求已提交");
            response.put("data", result);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
        }
        
        return response;
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
                String key = "post_list:" + page + ":" + size;
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
}