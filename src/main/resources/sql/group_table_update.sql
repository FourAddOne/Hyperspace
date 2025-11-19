-- 为现有的"group"表添加status字段
ALTER TABLE "group" 
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'NORMAL';

-- 为现有的"group_member"表添加status和mute_end_time字段
ALTER TABLE group_member 
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'NORMAL',
ADD COLUMN IF NOT EXISTS mute_end_time BIGINT;

-- 为"group_member"表创建状态索引
CREATE INDEX IF NOT EXISTS idx_group_member_status ON group_member (status);

-- 添加注释说明字段含义
COMMENT ON COLUMN "group".status IS '群组状态 (NORMAL: 正常, MUTE_ALL: 全体禁言, BANNED: 封禁)';
COMMENT ON COLUMN group_member.status IS '用户状态 (NORMAL: 正常, MUTED: 禁言)';
COMMENT ON COLUMN group_member.mute_end_time IS '禁言结束时间 (为NULL表示未禁言，-1表示永久禁言)';