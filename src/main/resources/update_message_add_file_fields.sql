-- 添加文件相关字段到message表
ALTER TABLE `message` 
ADD COLUMN `file_urls` VARCHAR(500) COMMENT '文件URL（type=file时必填）',
ADD COLUMN `file_name` VARCHAR(255) COMMENT '文件名（type=file时可选）',
ADD COLUMN `file_size` BIGINT COMMENT '文件大小（字节，type=file时可选）';