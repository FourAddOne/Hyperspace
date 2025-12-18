package com.lihuahua.hyperspace.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.models.entity.Post;
import com.lihuahua.hyperspace.models.entity.PostComment;
import com.lihuahua.hyperspace.models.entity.PostLike;
import com.lihuahua.hyperspace.models.entity.CommentLikeMessage;
import com.lihuahua.hyperspace.models.vo.ResVO;
import com.lihuahua.hyperspace.server.PostCommentServer;
import com.lihuahua.hyperspace.server.PostServer;
import com.lihuahua.hyperspace.service.CommentLikeRabbitMQService;
import com.lihuahua.hyperspace.utils.SnowflakeIdUtil;
import com.lihuahua.hyperspace.utils.OssUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    
    @Autowired
    private OssUtil ossUtil;

    // 评论列表缓存前缀
    private static final String COMMENT_LIST_PREFIX = "post_comments:";

    /**
     * 创建帖子
     *
     * @param post 帖子信息
     * @return 创建结果
     */
    @PostMapping("/create")
    public ResVO<?> createPost(@RequestBody Post post) {
        try{
            Map<String, Object> result = postServer.createPost(post);
            return ResVO.success(result.get("data"));
        }
        catch (Exception e){
            return ResVO.error(201,e.getMessage());
        }

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
    public ResVO<?> getPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        try {
            Map<String, Object> result = postServer.getPosts(page, size, category, type);
            return ResVO.success(result.get("data"));
        } catch (Exception e) {
            return ResVO.error(500, e.getMessage());
        }
    }

    /**
     * 获取帖子详情
     *
     * @param postId 帖子ID
     * @return 帖子详情
     */
    @GetMapping("/detail/{postId}")
    public ResVO<?>  getPostDetail(@PathVariable String postId, HttpServletRequest request) {
        // 从请求中获取当前用户信息
        String userId = (String) request.getAttribute("userId");
        Map<String, Object> result = postServer.getPostDetail(postId, userId);
        return ResVO.success(result.get("data"));
    }

    /**
     * 获取帖子评论列表
     *
     * @param postId 帖子ID
     * @return 评论列表
     */
    @GetMapping("/comments/{postId}")
    public ResVO<?> getPostComments(@PathVariable String postId, HttpServletRequest request) {
        // 从请求中获取当前用户信息
        String userId = (String) request.getAttribute("userId");
        try {
            List<PostComment> comments = postServer.getPostComments(postId, userId);
            return ResVO.success(comments);
        } catch (Exception e) {
            return ResVO.error(500, e.getMessage());
        }
    }
    
    /**
     * 上传评论图片
     *
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/comment/upload-image")
    public ResVO<?> uploadCommentImage(@RequestParam("file") MultipartFile file) {
        try {
            String result = postServer.uploadCommentImage(file);
            return ResVO.success(result);
        } catch (Exception e) {
            return ResVO.error(500, e.getMessage());
        }
    }
    
    /**
     * 创建评论
     *
     * @param postId 帖子ID
     * @param comment 评论内容
     * @return 评论结果
     */
    @PostMapping("/comment/create/{postId}")
    public ResVO<?> createComment(
            @PathVariable String postId, 
            @RequestBody PostComment comment) throws JsonProcessingException {
        try {
            PostComment result = postServer.createComment(postId, comment);
            return ResVO.success(result);
        } catch (Exception e) {
            return ResVO.error(500, e.getMessage());
        }
    }
    
    /**
     * 点赞评论（使用RabbitMQ异步处理）
     *
     * @param commentId 评论ID
     * @param like 点赞信息
     * @return 点赞结果
     */
    @PostMapping("/comment/like/{commentId}")
    public ResVO<?> likeComment(
            @PathVariable String commentId,
            @RequestBody Map<String, String> like) {
        try {
            String userId = like.get("userId");
            String username = like.get("username");
            
            // 使用基于RabbitMQ的异步方式处理点赞逻辑
            boolean result = postCommentServer.likeComment(commentId, userId, username);
            
            return ResVO.success(result);
        } catch (Exception e) {
            return ResVO.error(500, e.getMessage());
        }
    }
    
    /**
     * 回复评论
     *
     * @param commentId 被回复的评论ID
     * @param comment 回复内容
     * @return 回复结果
     */
    @PostMapping("/comment/reply/{commentId}")
    public ResVO<?> replyComment(
            @PathVariable String commentId,
            @RequestBody PostComment comment) throws JsonProcessingException {
        try {
            PostComment result = postServer.replyComment(commentId, comment);
            return ResVO.success(result);
        } catch (Exception e) {
            return ResVO.error(500, e.getMessage());
        }
    }
    
    /**
     * 点赞帖子
     *
     * @param postId 帖子ID
     * @param like 点赞信息
     * @return 点赞结果
     */
    @PostMapping("/like/{postId}")
    public ResVO<?> likePost(
            @PathVariable String postId,
            @RequestBody PostLike like) {
        try {
            // 使用基于RabbitMQ的异步方式处理点赞逻辑
            boolean result = postServer.toggleLikeWithRabbitMQ(postId, like.getUserId(), like.getUsername());
            
            return ResVO.success(result);
        } catch (Exception e) {
            return ResVO.error(500, e.getMessage());
        }
    }
}