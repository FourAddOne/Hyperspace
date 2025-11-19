-- 用户表
CREATE TABLE "user" (
    -- 数据库自增ID
    id BIGSERIAL PRIMARY KEY,
    
    -- 用户唯一标识
    user_id VARCHAR(64) NOT NULL UNIQUE,
    
    -- 用户名
    user_name VARCHAR(100) NOT NULL,
    
    -- 邮箱
    email VARCHAR(100) NOT NULL UNIQUE,
    
    -- 头像URL
    avatar_url VARCHAR(255),
    
    -- 密码
    password VARCHAR(255) NOT NULL,
    
    -- 注册IP
    register_ip VARCHAR(50),
    
    -- 登录IP
    login_ip VARCHAR(50),
    
    -- 最后阅读时间戳
    last_read_ts BIGINT,
    
    -- 登录状态
    login_status BOOLEAN DEFAULT FALSE,
    
    -- 创建时间
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 更新时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以提高查询性能
CREATE INDEX idx_user_user_id ON "user" (user_id);
CREATE INDEX idx_user_email ON "user" (email);
CREATE INDEX idx_user_user_name ON "user" (user_name);