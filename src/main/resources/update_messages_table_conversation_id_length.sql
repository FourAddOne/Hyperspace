-- Increase the size of conversation_id column to accommodate generated IDs
ALTER TABLE messages MODIFY COLUMN conversation_id VARCHAR(128) DEFAULT NULL COMMENT '会话ID';