-- 修复 learning_record 表缺少 update_time 和 deleted 字段的问题
-- 执行时间: 2026-06-26

ALTER TABLE `learning_record`
  ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`,
  ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除' AFTER `update_time`;
