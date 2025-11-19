-- 群组表
CREATE TABLE "group" (
    -- 数据库自增ID
    id BIGSERIAL PRIMARY KEY,
    
    -- 群组唯一标识
    group_id VARCHAR(64) NOT NULL UNIQUE,
    
    -- 群组名称
    group_name VARCHAR(100) NOT NULL,
    
    -- 创建者ID
    creator_id VARCHAR(64) NOT NULL,
    
    -- 群组状态 (NORMAL: 正常, MUTE_ALL: 全体禁言, BANNED: 封禁)
    status VARCHAR(20) DEFAULT 'NORMAL',
    
    -- 创建时间
    created_at BIGINT NOT NULL,
    
    -- 更新时间
    updated_at BIGINT NOT NULL
);

-- 群成员表
CREATE TABLE group_member (
    -- 数据库自增ID
    id BIGSERIAL PRIMARY KEY,
    
    -- 群组ID
    group_id VARCHAR(64) NOT NULL,

    -- 用户ID
    user_id VARCHAR(64) NOT NULL,
    
    -- 用户角色(MEMBER, ADMIN, OWNER)
    role VARCHAR(20) DEFAULT 'MEMBER',
    
    -- 用户状态 (NORMAL: 正常, MUTED: 禁言)
    status VARCHAR(20) DEFAULT 'NORMAL',
    
    -- 禁言结束时间 (为NULL表示未禁言，-1表示永久禁言)
    mute_end_time BIGINT,
    
    -- 加入时间
    joined_at BIGINT NOT NULL,
    
    -- 更新时间
    updated_at BIGINT NOT NULL,
    
    -- 群组ID和用户ID的唯一索引
    UNIQUE (group_id, user_id)
);

-- 创建索引以提高查询性能
CREATE INDEX idx_group_group_id ON "group" (group_id);
CREATE INDEX idx_group_member_user_id ON group_member (user_id);
CREATE INDEX idx_group_member_group_id ON group_member (group_id);
CREATE INDEX idx_group_member_status ON group_member (status);