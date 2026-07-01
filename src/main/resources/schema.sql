d:\CodeBase\ai-learning-platform\database\ai_companion.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 用户表（统一表：学生 + 管理员，通过 role 字段区分）
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`                  VARCHAR(50)  NOT NULL COMMENT '用户名(登录账号, 全局唯一)',
  `password`                  VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `nickname`                  VARCHAR(50)  DEFAULT '' COMMENT '昵称',
  `email`                     VARCHAR(100) DEFAULT '' COMMENT '邮箱',
  `phone`                     VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `avatar`                    VARCHAR(500) DEFAULT '' COMMENT '头像URL',
  `role`                      VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色: USER=学生, ADMIN=管理员',
  `status`                    TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
  `refresh_token`             VARCHAR(64)  DEFAULT NULL COMMENT '刷新令牌',
  `refresh_token_expire_time` DATETIME     DEFAULT NULL COMMENT '刷新令牌过期时间',
  `deleted`                   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
  `create_time`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_username` (`username`),
  INDEX `idx_role` (`role`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表(学生+管理员)';

-- 默认管理员账号（密码: 123456）
INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@aicompanion.com', 'ADMIN'),
('admin02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '运营管理员', 'admin02@aicompanion.com', 'ADMIN'),
('admin03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '内容管理员', 'admin03@aicompanion.com', 'ADMIN');

-- ============================================================
-- 2. 技能树表（自关联树形结构）
-- ============================================================
DROP TABLE IF EXISTS `skill`;
CREATE TABLE `skill` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '技能ID',
  `name`        VARCHAR(100) NOT NULL COMMENT '技能名称',
  `category`    VARCHAR(50)  NOT NULL COMMENT '技能类别(一级分类, 用于快速筛选)',
  `description` TEXT         DEFAULT NULL COMMENT '技能描述',
  `level`       TINYINT      DEFAULT 1 COMMENT '难度等级 1-5',
  `parent_id`   BIGINT       DEFAULT 0 COMMENT '父技能ID (0表示顶级)',
  `sort_order`  INT          DEFAULT 0 COMMENT '排序序号(同级内升序)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_parent_id` (`parent_id`),
  INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能树表';

INSERT INTO `skill` (`name`, `category`, `description`, `level`, `parent_id`, `sort_order`) VALUES
('Java基础', '后端开发', 'JavaSE核心语法', 1, 0, 1),
('Spring Boot', '后端开发', 'Spring Boot框架开发', 3, 1, 2),
('MySQL', '数据库', '关系型数据库', 2, 0, 1),
('Vue3', '前端开发', 'Vue3 + JavaScript', 2, 0, 1),
('Docker', '运维部署', '容器化技术', 3, 0, 1);

-- ============================================================
-- 3. 用户技能关联表（多对多中间表）
-- ============================================================
DROP TABLE IF EXISTS `user_skill`;
CREATE TABLE `user_skill` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID（逻辑关联 user.id）',
  `skill_id`    BIGINT   NOT NULL COMMENT '技能ID（逻辑关联 skill.id）',
  `level`       TINYINT  DEFAULT 0 COMMENT '掌握程度 0-5',
  `status`      TINYINT  DEFAULT 0 COMMENT '状态: 0=未开始, 1=学习中, 2=已掌握',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_skill` (`user_id`, `skill_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户技能关联表';

-- ============================================================
-- 4. 学习计划表（AI 伴学核心：规划→执行→反馈闭环）
-- ============================================================
DROP TABLE IF EXISTS `learning_plan`;
CREATE TABLE `learning_plan` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  `user_id`         BIGINT       NOT NULL COMMENT '用户ID（逻辑关联 user.id）',
  `title`           VARCHAR(200) NOT NULL COMMENT '计划标题',
  `target_position` VARCHAR(100) DEFAULT '' COMMENT '目标岗位(如: Java后端开发)',
  `description`     TEXT         DEFAULT NULL COMMENT '计划描述',
  `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=未开始, 1=进行中, 2=已完成, 3=已放弃',
  `start_time`      DATETIME     DEFAULT NULL COMMENT '计划开始时间',
  `end_time`        DATETIME     DEFAULT NULL COMMENT '计划结束时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';

-- ============================================================
-- 5. 学习计划-技能关联表
-- ============================================================
DROP TABLE IF EXISTS `learning_plan_skill`;
CREATE TABLE `learning_plan_skill` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `plan_id`      BIGINT   NOT NULL COMMENT '计划ID（逻辑关联 learning_plan.id）',
  `skill_id`     BIGINT   NOT NULL COMMENT '技能ID（逻辑关联 skill.id）',
  `sort_order`   INT      DEFAULT 0 COMMENT '学习顺序',
  `target_level` TINYINT  DEFAULT 3 COMMENT '目标掌握程度 1-5',
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_plan_skill` (`plan_id`, `skill_id`),
  INDEX `idx_plan_id` (`plan_id`),
  INDEX `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划-技能关联表';

-- ============================================================
-- 6. 学习记录表（反范式化设计：冗余 skill_name 减少 JOIN）
-- ============================================================
DROP TABLE IF EXISTS `learning_record`;
CREATE TABLE `learning_record` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`          BIGINT       NOT NULL COMMENT '用户ID（逻辑关联 user.id）',
  `skill_id`         BIGINT       NOT NULL COMMENT '学习资源ID（逻辑关联 skill.id）',
  `skill_name`       VARCHAR(200) DEFAULT '' COMMENT '资源名称(冗余字段, 减少JOIN, 历史数据快照)',
  `source_type`      VARCHAR(20)  DEFAULT 'ARTICLE' COMMENT '来源类型: VIDEO/ARTICLE/PRACTICE/AI_CHAT',
  `progress`         INT          DEFAULT 0 COMMENT '进度百分比 0-100',
  `study_seconds`    INT          DEFAULT 0 COMMENT '累计学习秒数',
  `status`           TINYINT      DEFAULT 0 COMMENT '状态: 0=未开始, 1=学习中, 2=已完成',
  `first_study_time` DATETIME     DEFAULT NULL COMMENT '首次学习时间',
  `last_study_time`  DATETIME     DEFAULT NULL COMMENT '最近学习时间',
  `complete_time`    DATETIME     DEFAULT NULL COMMENT '完成时间',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_skill_id` (`skill_id`),
  INDEX `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';

-- ============================================================
-- 7. AI 对话会话表（ChatGPT 式多对话窗口）
-- ============================================================
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id`     BIGINT       NOT NULL COMMENT '用户ID（逻辑关联 user.id）',
  `title`       VARCHAR(200) DEFAULT '' COMMENT '会话标题(自动生成或用户自定义)',
  `agent_type`  VARCHAR(30)  NOT NULL DEFAULT 'CHAT' COMMENT 'Agent类型: CHAT/ASSESSMENT/PLANNING/INTERVIEW',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_agent_type` (`agent_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- ============================================================
-- 8. AI 对话消息表
-- ============================================================
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id`  BIGINT       NOT NULL COMMENT '会话ID（逻辑关联 chat_session.id）',
  `user_id`     BIGINT       NOT NULL COMMENT '用户ID（逻辑关联 user.id）',
  `role`        VARCHAR(20)  NOT NULL COMMENT '角色: user/assistant/system',
  `content`     TEXT         NOT NULL COMMENT '消息内容',
  `model`       VARCHAR(50)  DEFAULT '' COMMENT '使用的模型',
  `tokens_used` INT          DEFAULT 0 COMMENT '消耗Token数',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_session_id` (`session_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

-- ============================================================
-- 9. 考核会话表
-- ============================================================
DROP TABLE IF EXISTS `exam_session`;
CREATE TABLE `exam_session` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id`          BIGINT       NOT NULL COMMENT '用户ID',
  `skill_id`         BIGINT       NOT NULL COMMENT '技能ID',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS/PASSED/FAILED',
  `total_score`      INT          DEFAULT 0 COMMENT '总分',
  `pass_score`       INT          NOT NULL DEFAULT 70 COMMENT '及格线(百分比)',
  `total_questions`  INT          NOT NULL DEFAULT 5 COMMENT '总题数',
  `answered_count`   INT          DEFAULT 0 COMMENT '已答数',
  `ai_feedback`      TEXT         DEFAULT NULL COMMENT 'AI综合评价',
  `start_time`       DATETIME     DEFAULT NULL COMMENT '开始时间',
  `end_time`         DATETIME     DEFAULT NULL COMMENT '结束时间',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_user_skill` (`user_id`, `skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核会话表';