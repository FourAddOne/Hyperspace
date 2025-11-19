-- 好友关系表
CREATE TABLE friend (
    -- 数据库自增ID
    id BIGSERIAL PRIMARY KEY,
    
    -- 用户1ID（较小的用户ID）
    user_one VARCHAR(64) NOT NULL,
    
    -- 用户2ID（较大的用户ID）
    user_sec VARCHAR(64) NOT NULL,
    
    -- 好友状态 (PENDING: 等待确认, ACCEPTED: 已接受, REJECTED: 已拒绝, BLOCKED: 已屏蔽)
    status VARCHAR(20) DEFAULT 'PENDING',
    
    -- 屏蔽状态 (NONE: 无屏蔽, USER_ONE_BLOCKED: 用户1屏蔽了用户2, USER_SEC_BLOCKED: 用户2屏蔽了用户1, BOTH_BLOCKED: 双方互相屏蔽)
    block_status VARCHAR(20) DEFAULT 'NONE',
    
    -- 用户1对用户2的备注名称
    user_one_remark VARCHAR(100),
    
    -- 用户2对用户1的备注名称
    user_sec_remark VARCHAR(100),
    
    -- 创建时间
    created_at BIGINT NOT NULL,
    
    -- 更新时间
    updated_at BIGINT NOT NULL,
    
    -- 唯一索引确保两个用户之间的关系唯一
    UNIQUE (user_one, user_sec)
);

-- 创建索引以提高查询性能
CREATE INDEX idx_friend_user_one ON friend (user_one);
CREATE INDEX idx_friend_user_sec ON friend (user_sec);
CREATE INDEX idx_friend_status ON friend (status);
CREATE INDEX idx_friend_block_status ON friend (block_status);
CREATE INDEX idx_friend_created_at ON friend (created_at);

-- 添加注释说明字段含义
COMMENT ON COLUMN friend.user_one IS '用户1ID（较小的用户ID）';
COMMENT ON COLUMN friend.user_sec IS '用户2ID（较大的用户ID）';
COMMENT ON COLUMN friend.status IS '好友状态 (PENDING: 等待确认, ACCEPTED: 已接受, REJECTED: 已拒绝, BLOCKED: 已屏蔽)';
COMMENT ON COLUMN friend.block_status IS '屏蔽状态 (NONE: 无屏蔽, USER_ONE_BLOCKED: 用户1屏蔽了用户2, USER_SEC_BLOCKED: 用户2屏蔽了用户1, BOTH_BLOCKED: 双方互相屏蔽)';
COMMENT ON COLUMN friend.user_one_remark IS '用户1对用户2的备注名称';
COMMENT ON COLUMN friend.user_sec_remark IS '用户2对用户1的备注名称';
COMMENT ON COLUMN friend.created_at IS '创建时间';
COMMENT ON COLUMN friend.updated_at IS '更新时间';

-- 针对 user_one 场景：包含过滤条件和所需字段
CREATE INDEX idx_friend_one_status_block ON friend (user_one, status, block_status)
INCLUDE (user_sec, user_one_remark, user_sec_remark); -- PostgreSQL 支持 INCLUDE 包含非索引列

-- 针对 user_sec 场景：同理
CREATE INDEX idx_friend_sec_status_block ON friend (user_sec, status, block_status)
INCLUDE (user_one, user_one_remark, user_sec_remark);