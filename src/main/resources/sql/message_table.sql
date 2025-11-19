-- 消息表
CREATE TABLE "message" (
    -- 数据库自增ID
    id BIGSERIAL PRIMARY KEY,
    
    -- 消息唯一标识
    message_id VARCHAR(64) NOT NULL UNIQUE,
    
    -- 消息类型 (TEXT, IMAGE, MIXED, SYSTEM等)
    type VARCHAR(20) NOT NULL,
    
    -- 发送者ID
    from_user_id VARCHAR(64) NOT NULL,
    
    -- 发送者名称
    from_username VARCHAR(100),
    
    -- 接收者ID (用户ID或群组ID)
    to_target_id VARCHAR(64) NOT NULL,
    
    -- 接收者类型 (USER, GROUP)
    to_target_type VARCHAR(20) NOT NULL,
    
    -- 接收者名称 (用户名或群组名)
    to_target_name VARCHAR(100),
    
    -- 文本内容
    text_content TEXT,
    
    -- 图片URL (JSON格式存储多个图片URL)
    image_urls TEXT,
    
    -- 客户端时间戳
    client_timestamp BIGINT NOT NULL,
    
    -- 服务器时间戳
    server_timestamp BIGINT NOT NULL,
    
    -- 消息状态 (SENDING, DELIVERED, READ, FAILED)
    status VARCHAR(20) NOT NULL,
    
    -- 引用消息ID (用于回复消息)
    quote_message_id VARCHAR(64),
    
    -- 设备类型
    device_type VARCHAR(50),
    
    -- 设备名称
    device_name VARCHAR(100),
    
    -- 创建时间
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 更新时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以提高查询性能
CREATE INDEX idx_message_message_id ON "message" (message_id);
CREATE INDEX idx_message_from_user_id ON "message" (from_user_id);
CREATE INDEX idx_message_to_target_id ON "message" (to_target_id);
CREATE INDEX idx_message_client_timestamp ON "message" (client_timestamp);
CREATE INDEX idx_message_server_timestamp ON "message" (server_timestamp);
CREATE INDEX idx_message_status ON "message" (status);