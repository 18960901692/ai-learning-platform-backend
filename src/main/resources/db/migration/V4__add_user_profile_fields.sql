-- 为用户表添加 profession（职业）和 bio（个人简介）字段
ALTER TABLE `user`
  ADD COLUMN `profession` VARCHAR(100) DEFAULT NULL COMMENT '职业' AFTER `avatar`,
  ADD COLUMN `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介' AFTER `profession`;
