-- 为messages表添加receiver_id字段
ALTER TABLE messages ADD COLUMN receiver_id VARCHAR(32) NOT NULL AFTER sender_id;