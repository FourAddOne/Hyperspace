-- 创建消息表
CREATE TABLE IF NOT EXISTS `messages` (
  `message_id` varchar(64) NOT NULL COMMENT '消息ID',
  `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `sender_id` varchar(64) NOT NULL COMMENT '发送者ID',
  `receiver_id` varchar(64) NOT NULL COMMENT '接收者ID',
  `content_type` varchar(20) DEFAULT 'text' COMMENT '内容类型(text,image,file,emoji,voice,video)',
  `content` text COMMENT '消息内容',
  `file_url` varchar(255) DEFAULT NULL COMMENT '文件URL',
  `status` varchar(20) DEFAULT 'sent' COMMENT '消息状态(sent,delivered,read)',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
  PRIMARY KEY (`message_id`),
  KEY `idx_sender_receiver` (`sender_id`,`receiver_id`),
  KEY `idx_timestamp` (`timestamp`),
  KEY `idx_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';