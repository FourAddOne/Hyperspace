package com.lihuahua.hyperspace.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lihuahua.hyperspace.models.entity.Post;
import com.lihuahua.hyperspace.models.entity.PostComment;
import com.lihuahua.hyperspace.server.PostServer;
import com.lihuahua.hyperspace.server.PostCommentServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存同步任务
 * 定期同步数据库和Redis缓存中的数据，保证数据一致性
 */
@Component
public class CacheSyncTask {

    @Autowired
    private PostServer postServer;

    @Autowired
    private PostCommentServer postCommentServer;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String POST_DETAIL_PREFIX = "post_detail:";
    private static final String COMMENT_LIST_PREFIX = "post_comments:";
    private static final String LIKE_COUNT_PREFIX = "post_like_count:";
    private static final String COMMENT_LIKE_COUNT_PREFIX = "comment_like_count:";

    /**
     * 每30分钟同步一次热门帖子的缓存数据
     * 同步点赞数较高的帖子，确保热点数据在缓存中
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟执行一次
    public void syncHotPostCache() {
        try {
            System.out.println("开始同步热门帖子缓存数据...");

            // 查询最近24小时内点赞数较多的帖子（热门帖子）
            QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
            queryWrapper.ge("like_count", 0); // 点赞数大于等于10的帖子
            queryWrapper.orderByDesc("like_count");
            queryWrapper.last("LIMIT 100"); // 最多同步100个帖子

            List<Post> hotPosts = postServer.list(queryWrapper);

            for (Post post : hotPosts) {
                // 同步帖子详情到缓存
                String postDetailKey = POST_DETAIL_PREFIX + post.getPostId();
                redisTemplate.opsForValue().set(
                        postDetailKey,
                        objectMapper.writeValueAsString(post),
                        60, // 60分钟过期
                        TimeUnit.MINUTES
                );

                // 同步帖子点赞数到缓存
                String likeCountKey = LIKE_COUNT_PREFIX + post.getPostId();
                redisTemplate.opsForValue().set(
                        likeCountKey,
                        String.valueOf(post.getLikeCount()),
                        60, // 60分钟过期
                        TimeUnit.MINUTES
                );

                // 同步评论列表到缓存
                syncCommentsCache(post.getPostId());
            }

            System.out.println("热门帖子缓存数据同步完成，共同步 " + hotPosts.size() + " 个帖子");
        } catch (Exception e) {
            System.err.println("同步热门帖子缓存数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 每天凌晨同步所有帖子的缓存数据
     * 确保即使长期未访问的帖子也能得到同步
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void syncAllPostCache() {
        try {
            System.out.println("开始同步所有帖子缓存数据...");

            // 分批处理所有帖子，避免一次性加载过多数据
            int pageSize = 100;
            int currentPage = 0;
            int totalSynced = 0;

            while (true) {
                // 分页查询帖子
                QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
                queryWrapper.orderByAsc("id");
                queryWrapper.last("LIMIT " + pageSize + " OFFSET " + (currentPage * pageSize));

                List<Post> posts = postServer.list(queryWrapper);
                
                // 如果没有更多数据，退出循环
                if (posts.isEmpty()) {
                    break;
                }

                // 同步这批帖子的缓存
                for (Post post : posts) {
                    // 同步帖子详情到缓存
                    String postDetailKey = POST_DETAIL_PREFIX + post.getPostId();
                    redisTemplate.opsForValue().set(
                            postDetailKey,
                            objectMapper.writeValueAsString(post),
                            24 * 60, // 24小时过期
                            TimeUnit.MINUTES
                    );

                    // 同步帖子点赞数到缓存
                    String likeCountKey = LIKE_COUNT_PREFIX + post.getPostId();
                    redisTemplate.opsForValue().set(
                            likeCountKey,
                            String.valueOf(post.getLikeCount()),
                            24 * 60, // 24小时过期
                            TimeUnit.MINUTES
                    );

                    // 同步评论列表到缓存
                    syncCommentsCache(post.getPostId());
                }

                totalSynced += posts.size();
                currentPage++;
                
                // 避免过于频繁的数据库查询，适当休眠
                Thread.sleep(1000);
            }

            System.out.println("所有帖子缓存数据同步完成，共同步 " + totalSynced + " 个帖子");
        } catch (Exception e) {
            System.err.println("同步所有帖子缓存数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 同步指定帖子的评论列表到缓存
     * @param postId 帖子ID
     */
    private void syncCommentsCache(String postId) {
        try {
            // 现在可以正确调用这个方法了
            List<PostComment> comments = postCommentServer.getCommentsByPostId(postId);
            String commentListKey = COMMENT_LIST_PREFIX + postId;
            
            redisTemplate.opsForValue().set(
                    commentListKey,
                    objectMapper.writeValueAsString(comments),
                    30, // 30分钟过期
                    TimeUnit.MINUTES
            );

            // 同步每条评论的点赞数
            for (PostComment comment : comments) {
                String commentLikeCountKey = COMMENT_LIKE_COUNT_PREFIX + comment.getCommentId();
                redisTemplate.opsForValue().set(
                        commentLikeCountKey,
                        String.valueOf(comment.getLikeCount()),
                        30, // 30分钟过期
                        TimeUnit.MINUTES
                );
            }
        } catch (Exception e) {
            System.err.println("同步帖子 " + postId + " 的评论缓存时出错: " + e.getMessage());
        }
    }
}