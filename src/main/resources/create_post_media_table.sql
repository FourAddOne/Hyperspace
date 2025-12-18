-- 帖子媒体资源表
CREATE TABLE `post_media` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '数据库内部自增主键',
  `media_id` varchar(64) NOT NULL COMMENT '媒体全局唯一ID（业务主键）',
  `post_id` varchar(64) NOT NULL COMMENT '所属帖子ID',
  `type` varchar(20) NOT NULL COMMENT '媒体类型（image-图片、video-视频）',
  `url` varchar(255) NOT NULL COMMENT '媒体URL',
  `thumbnail_url` varchar(255) DEFAULT NULL COMMENT '缩略图URL（视频专用）',
  `width` int DEFAULT NULL COMMENT '宽度（像素）',
  `height` int DEFAULT NULL COMMENT '高度（像素）',
  `size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `duration` int DEFAULT NULL COMMENT '时长（秒，视频专用）',
  `sort_order` int DEFAULT '0' COMMENT '排序顺序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_id` (`media_id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_type` (`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHAR    SET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子媒体资源表';