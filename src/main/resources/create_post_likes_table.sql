-- 帖子点赞表
CREATE TABLE `post_like` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '数据库内部自增主键',
  `like_id` varchar(64) NOT NULL COMMENT '点赞全局唯一ID（业务主键）',
  `post_id` varchar(64) NOT NULL COMMENT '被点赞的帖子ID',
  `user_id` varchar(64) NOT NULL COMMENT '点赞用户ID',
  `username` varchar(100) NOT NULL COMMENT '点赞用户名（冗余存储）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_like_id` (`like_id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表';