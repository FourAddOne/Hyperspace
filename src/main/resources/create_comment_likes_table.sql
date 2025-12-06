CREATE TABLE `comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `like_id` varchar(64) NOT NULL COMMENT '点赞ID',
  `comment_id` varchar(64) NOT NULL COMMENT '评论ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `username` varchar(100) NOT NULL COMMENT '用户名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_like_comment_user` (`comment_id`,`user_id`),
  KEY `idx_comment_like_comment_id` (`comment_id`),
  KEY `idx_comment_like_user_id` (`user_id`),
  KEY `idx_comment_like_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';