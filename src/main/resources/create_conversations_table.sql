-- Create conversations table
CREATE TABLE IF NOT EXISTS `conversations` (
  `conversation_id` varchar(32) NOT NULL COMMENT '会话ID',
  `type` enum('private','group') NOT NULL COMMENT '会话类型(private:私人对话,group:群组对话)',
  `name` varchar(100) DEFAULT NULL COMMENT '会话名称(群组对话时使用)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';