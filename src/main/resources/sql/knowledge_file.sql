-- 知识库文件表
CREATE TABLE IF NOT EXISTS `knowledge_file` (
    `id` BIGINT NOT NULL COMMENT '主键ID（雪花算法）',
    `original_filename` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `stored_filename` VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    `file_type` VARCHAR(20) DEFAULT NULL COMMENT '文件类型（扩展名）',
    `chunk_count` INT DEFAULT 0 COMMENT '切分片段数',
    `chunk_doc_ids` TEXT COMMENT '向量库文档ID列表（JSON数组）',
    `user_id` BIGINT DEFAULT NULL COMMENT '上传用户ID',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文件表';
