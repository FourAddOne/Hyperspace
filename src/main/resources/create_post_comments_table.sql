-- 帖子评论表
CREATE TABLE `post_comment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '数据库内部自增主键',
  `comment_id` varchar(64) NOT NULL COMMENT '评论全局唯一ID（业务主键）',
  `post_id` varchar(64) NOT NULL COMMENT '被评论的帖子ID',
  `user_id` varchar(64) NOT NULL COMMENT '评论用户ID',
  `username` varchar(100) NOT NULL COMMENT '评论用户名',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '评论用户头像URL',
  `content` text NOT NULL COMMENT '评论内容',
  `parent_id` varchar(64) DEFAULT NULL COMMENT '父评论ID（用于回复评论）',
  `reply_to_user_id` varchar(64) DEFAULT NULL COMMENT '被回复的用户ID（用于回复评论）',
  `reply_to_username` varchar(100) DEFAULT NULL COMMENT '被回复的用户名（用于回复评论）',
  `like_count` int DEFAULT '0' COMMENT '评论点赞数',
  `status` varchar(20) NOT NULL DEFAULT 'normal' COMMENT '评论状态（normal-正常、deleted-已删除）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_id` (`comment_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子评论表';