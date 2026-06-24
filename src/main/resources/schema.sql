d:\CodeBase\ai-learning-platform\database\ai_companion.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 用户表（学生端）
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
  `status`                    TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
  `refresh_token`             VARCHAR(64)  DEFAULT NULL COMMENT '刷新令牌',
  `refresh_token_expire_time` DATETIME     DEFAULT NULL COMMENT '刷新令牌过期时间',
  `deleted`                   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
  `create_time`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_username` (`username`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表(学生)';

-- ============================================================
-- 2. 管理员表
-- ============================================================
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id`                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username`                  VARCHAR(50)  NOT NULL COMMENT '用户名(登录账号, 全局唯一)',
  `password`                  VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `nickname`                  VARCHAR(50)  DEFAULT '' COMMENT '昵称',
  `email`                     VARCHAR(100) DEFAULT '' COMMENT '邮箱',
  `phone`                     VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `avatar`                    VARCHAR(500) DEFAULT '' COMMENT '头像URL',
  `status`                    TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
  `refresh_token`             VARCHAR(64)  DEFAULT NULL COMMENT '刷新令牌',
  `refresh_token_expire_time` DATETIME     DEFAULT NULL COMMENT '刷新令牌过期时间',
  `deleted`                   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删除',
  `create_time`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_username` (`username`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

INSERT INTO `admin` (`username`, `password`, `nickname`, `email`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@aicompanion.com');

-- ============================================================
-- 3. 技能树表（自关联树形结构）
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
-- 4. 用户技能关联表（多对多中间表）
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
-- 5. 学习计划表（AI 伴学核心：规划→执行→反馈闭环）
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
-- 6. 学习计划-技能关联表
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
-- 7. 学习记录表（反范式化设计：冗余 skill_name 减少 JOIN）
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
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_skill_id` (`skill_id`),
  INDEX `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';

-- ============================================================
-- 8. AI 对话会话表（ChatGPT 式多对话窗口）
-- ============================================================
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id`     BIGINT       NOT NULL COMMENT '用户ID（逻辑关联 user.id）',
  `title`       VARCHAR(200) DEFAULT '' COMMENT '会话标题(自动生成或用户自定义)',
  `agent_type`  VARCHAR(30)  NOT NULL DEFAULT 'CHAT' COMMENT 'Agent类型: CHAT/ASSESSMENT/PLANNING/INTERVIEW',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_agent_type` (`agent_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- ============================================================
-- 9. AI 对话消息表
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
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_session_id` (`session_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

-- ============================================================
-- 10. Dify 工作流记录表
-- ============================================================
DROP TABLE IF EXISTS `dify_workflow`;
CREATE TABLE `dify_workflow` (
  `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`              BIGINT       NOT NULL COMMENT '用户ID（逻辑关联 user.id）',
  `workflow_type`        VARCHAR(30)  NOT NULL COMMENT '类型: RESUME/REPORT/CONTENT',
  `input_params`         JSON         DEFAULT NULL COMMENT '输入参数(JSON)',
  `output_result`        JSON         DEFAULT NULL COMMENT '输出结果(JSON)',
  `status`               VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/COMPLETED/FAILED',
  `dify_conversation_id` VARCHAR(100) DEFAULT NULL COMMENT 'Dify会话ID',
  `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_workflow_type` (`workflow_type`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dify工作流记录表';

-- ============================================================
-- 11. 面试会话表
-- ============================================================
DROP TABLE IF EXISTS `interview_session`;
CREATE TABLE `interview_session` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id`          BIGINT       NOT NULL COMMENT '用户ID（逻辑关联 user.id）',
  `job_position`     VARCHAR(100) NOT NULL COMMENT '应聘岗位',
  `difficulty`       TINYINT      NOT NULL DEFAULT 3 COMMENT '难度 1-5',
  `total_questions`  INT          NOT NULL DEFAULT 5 COMMENT '总题目数',
  `current_question` INT          NOT NULL DEFAULT 0 COMMENT '当前已答题目序号',
  `score`            INT          DEFAULT NULL COMMENT '面试总分',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS/COMPLETED',
  `feedback`         TEXT         DEFAULT NULL COMMENT 'AI总体评价',
  `start_time`       DATETIME     DEFAULT NULL COMMENT '开始时间',
  `end_time`         DATETIME     DEFAULT NULL COMMENT '结束时间',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI面试会话表';

-- ============================================================
-- 12. 面试题目表
-- ============================================================
DROP TABLE IF EXISTS `interview_question`;
CREATE TABLE `interview_question` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `session_id`     BIGINT   NOT NULL COMMENT '面试会话ID（逻辑关联 interview_session.id）',
  `question_order` INT      NOT NULL COMMENT '题目序号',
  `question`       TEXT     NOT NULL COMMENT '面试题目',
  `user_answer`    TEXT     DEFAULT NULL COMMENT '用户作答',
  `ai_evaluation`  TEXT     DEFAULT NULL COMMENT 'AI评价',
  `score`          INT      DEFAULT NULL COMMENT '该题得分',
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题目表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 测试数据
-- ============================================================

-- 1. user 表测试数据（8条）
INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `phone`) VALUES
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', 'zhangsan@test.com', '13800001001'),
('lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', 'lisi@test.com', '13800001002'),
('wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', 'wangwu@test.com', '13800001003'),
('zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', 'zhaoliu@test.com', '13800001004'),
('sunqi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '孙七', 'sunqi@test.com', '13800001005'),
('zhouba', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周八', 'zhouba@test.com', '13800001006'),
('wujiu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '吴九', 'wujiu@test.com', '13800001007'),
('zhengshi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '郑十', 'zhengshi@test.com', '13800001008');

-- 2. admin 表已有 1 条，再插入 2 条
INSERT INTO `admin` (`username`, `password`, `nickname`, `email`) VALUES
('admin02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '运营管理员', 'admin02@aicompanion.com'),
('admin03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '内容管理员', 'admin03@aicompanion.com');

-- 3. skill 表补充数据（已有5条，再插入5条共10条）
INSERT INTO `skill` (`name`, `category`, `description`, `level`, `parent_id`, `sort_order`) VALUES
('Spring Cloud', '后端开发', '微服务框架', 4, 1, 3),
('Redis', '数据库', '内存数据库/缓存', 3, 3, 2),
('React', '前端开发', '前端UI库', 3, 4, 2),
('Linux', '运维部署', '服务器操作系统', 2, 5, 2),
('Python', '后端开发', 'Python编程语言', 2, 0, 2);

-- 4. user_skill 表测试数据（8条）
INSERT INTO `user_skill` (`user_id`, `skill_id`, `level`, `status`) VALUES
(1, 1, 3, 2),
(1, 2, 2, 1),
(1, 3, 4, 2),
(2, 1, 2, 1),
(2, 4, 3, 1),
(3, 5, 1, 1),
(3, 3, 2, 1),
(4, 1, 4, 2);

-- 5. learning_plan 表测试数据（5条）
INSERT INTO `learning_plan` (`user_id`, `title`, `target_position`, `description`, `status`, `start_time`, `end_time`) VALUES
(1, 'Java后端校招计划', 'Java后端开发工程师', '系统学习Java后端技术栈，备战2026届校招', 1, '2026-03-01 00:00:00', '2026-09-01 00:00:00'),
(1, 'Spring Boot进阶', 'Java后端开发工程师', '深入学习Spring Boot微服务开发', 1, '2026-04-01 00:00:00', '2026-06-01 00:00:00'),
(2, '前端全栈计划', '前端开发工程师', '从Vue3到React全面掌握前端技术', 1, '2026-03-15 00:00:00', '2026-08-15 00:00:00'),
(3, '数据库专项提升', 'DBA/后端开发', '深入学习MySQL和Redis', 0, NULL, NULL),
(4, '运维部署入门', 'DevOps工程师', '掌握Docker和Linux基础', 2, '2026-01-01 00:00:00', '2026-03-01 00:00:00');

-- 6. learning_plan_skill 表测试数据（8条）
INSERT INTO `learning_plan_skill` (`plan_id`, `skill_id`, `sort_order`, `target_level`) VALUES
(1, 1, 1, 4),
(1, 2, 2, 3),
(1, 3, 3, 4),
(1, 6, 4, 3),
(2, 2, 1, 4),
(2, 6, 2, 3),
(3, 4, 1, 4),
(3, 7, 2, 3);

-- 7. learning_record 表测试数据（8条）
INSERT INTO `learning_record` (`user_id`, `skill_id`, `skill_name`, `source_type`, `progress`, `study_seconds`, `status`, `first_study_time`, `last_study_time`, `complete_time`) VALUES
(1, 1, 'Java基础', 'VIDEO', 85, 7200, 1, '2026-03-05 10:00:00', '2026-06-20 15:30:00', NULL),
(1, 2, 'Spring Boot', 'ARTICLE', 60, 3600, 1, '2026-04-10 09:00:00', '2026-06-18 14:00:00', NULL),
(1, 3, 'MySQL', 'PRACTICE', 100, 10800, 2, '2026-03-01 08:00:00', '2026-05-20 16:00:00', '2026-05-20 16:00:00'),
(2, 1, 'Java基础', 'AI_CHAT', 40, 1800, 1, '2026-05-01 10:00:00', '2026-06-22 11:00:00', NULL),
(2, 4, 'Vue3', 'VIDEO', 70, 5400, 1, '2026-03-20 14:00:00', '2026-06-19 16:00:00', NULL),
(3, 5, 'Docker', 'ARTICLE', 20, 900, 1, '2026-06-01 09:00:00', '2026-06-20 10:00:00', NULL),
(3, 3, 'MySQL', 'PRACTICE', 50, 2700, 1, '2026-05-15 13:00:00', '2026-06-21 15:00:00', NULL),
(4, 1, 'Java基础', 'VIDEO', 100, 14400, 2, '2026-01-10 08:00:00', '2026-02-28 17:00:00', '2026-02-28 17:00:00');

-- 8. chat_session 表测试数据（6条）
INSERT INTO `chat_session` (`user_id`, `title`, `agent_type`) VALUES
(1, 'Java面试准备', 'INTERVIEW'),
(1, 'Redis学习路线', 'PLANNING'),
(1, 'Spring Boot问题答疑', 'CHAT'),
(2, 'Vue3组件开发', 'CHAT'),
(2, '前端技能评估', 'ASSESSMENT'),
(3, 'Docker部署问题', 'CHAT');

-- 9. chat_message 表测试数据（10条）
INSERT INTO `chat_message` (`session_id`, `user_id`, `role`, `content`, `model`, `tokens_used`) VALUES
(1, 1, 'user', '你好，我想准备Java后端面试，能帮我模拟一下吗？', 'gpt-4', 25),
(1, 1, 'assistant', '当然可以！我们先从Java基础开始。请问你知道HashMap的底层实现原理吗？', 'gpt-4', 30),
(1, 1, 'user', 'HashMap底层是数组+链表+红黑树，JDK8之后引入了红黑树优化。', 'gpt-4', 28),
(1, 1, 'assistant', '回答得很好！那你知道HashMap的扩容机制吗？', 'gpt-4', 22),
(2, 1, 'user', '帮我制定一个Redis学习计划', 'gpt-4', 15),
(2, 1, 'assistant', '好的，Redis学习可以分为以下几个阶段：1. 基础数据类型 2. 持久化机制 3. 集群部署...', 'gpt-4', 45),
(3, 1, 'user', 'Spring Boot自动配置原理是什么？', 'gpt-4', 12),
(3, 1, 'assistant', 'Spring Boot自动配置核心是@EnableAutoConfiguration注解，通过SpringFactoriesLoader加载配置类...', 'gpt-4', 50),
(4, 2, 'user', 'Vue3的Composition API和Options API有什么区别？', 'gpt-4', 18),
(5, 2, 'user', '帮我评估一下我的前端技能水平', 'gpt-4', 15);

-- 10. dify_workflow 表测试数据（5条）
INSERT INTO `dify_workflow` (`user_id`, `workflow_type`, `input_params`, `output_result`, `status`, `dify_conversation_id`) VALUES
(1, 'RESUME', '{"position": "Java后端开发", "experience": "2年"}', '{"score": 75, "suggestions": ["增加项目经验描述", "突出技术栈"]}', 'COMPLETED', 'dify-conv-001'),
(1, 'RESUME', '{"position": "Java后端开发", "experience": "2年"}', NULL, 'RUNNING', 'dify-conv-002'),
(2, 'REPORT', '{"skill": "Vue3", "period": "2026-Q2"}', '{"progress": 70, "weak_points": ["TypeScript", "性能优化"]}', 'COMPLETED', 'dify-conv-003'),
(3, 'CONTENT', '{"topic": "Docker入门教程"}', NULL, 'PENDING', NULL),
(4, 'RESUME', '{"position": "DevOps工程师", "experience": "1年"}', '{"score": 60, "suggestions": ["补充CI/CD经验"]}', 'FAILED', 'dify-conv-004');

-- 11. interview_session 表测试数据（5条）
INSERT INTO `interview_session` (`user_id`, `job_position`, `difficulty`, `total_questions`, `current_question`, `score`, `status`, `feedback`, `start_time`, `end_time`) VALUES
(1, 'Java后端开发工程师', 3, 5, 5, 82, 'COMPLETED', '基础扎实，建议加强微服务架构方面的学习', '2026-06-15 10:00:00', '2026-06-15 10:45:00'),
(1, 'Java后端开发工程师', 4, 5, 3, NULL, 'IN_PROGRESS', NULL, '2026-06-22 14:00:00', NULL),
(2, '前端开发工程师', 3, 5, 5, 75, 'COMPLETED', 'Vue3掌握较好，React需要加强', '2026-06-18 09:00:00', '2026-06-18 09:40:00'),
(3, 'DBA工程师', 2, 5, 2, NULL, 'IN_PROGRESS', NULL, '2026-06-20 15:00:00', NULL),
(4, 'DevOps工程师', 3, 5, 5, 68, 'COMPLETED', 'Linux基础不错，Docker需要深入学习', '2026-06-10 11:00:00', '2026-06-10 11:50:00');

-- 12. interview_question 表测试数据（10条）
INSERT INTO `interview_question` (`session_id`, `question_order`, `question`, `user_answer`, `ai_evaluation`, `score`) VALUES
(1, 1, '请简述Java中HashMap的底层实现原理', 'HashMap底层是数组+链表+红黑树，JDK8之后引入了红黑树优化', '回答准确，提到了关键的数据结构变化', 90),
(1, 2, 'Spring Boot的自动配置原理是什么？', '通过@EnableAutoConfiguration和SpringFactoriesLoader实现', '回答简洁但缺少细节，可以补充条件注解的作用', 75),
(1, 3, 'MySQL的索引数据结构是什么？为什么用B+树？', 'B+树，因为范围查询效率高', '正确但不够完整，可以补充叶子节点链表的特点', 80),
(1, 4, 'Redis有哪些持久化方式？', 'RDB和AOF两种方式', '回答正确，建议补充两种方式的优缺点对比', 85),
(1, 5, '什么是微服务？Spring Cloud有哪些核心组件？', '微服务是将应用拆分为多个小服务，Spring Cloud有Eureka、Feign、Gateway等', '回答全面，体现了良好的知识体系', 90),
(3, 1, 'Vue3相比Vue2有哪些主要改进？', 'Composition API、更好的TypeScript支持、性能提升', '回答准确', 85),
(3, 2, '请解释Vue的响应式原理', '基于Object.defineProperty和Proxy实现数据劫持', '正确，可以补充Dep和Watcher的作用', 80),
(3, 3, 'React和Vue的主要区别是什么？', 'React是JSX+虚拟DOM，Vue是模板+响应式系统', '回答简洁到位', 75),
(5, 1, 'Docker和虚拟机的区别是什么？', 'Docker是容器化，共享宿主机内核，更轻量', '回答正确', 80),
(5, 2, '请简述Docker的镜像分层原理', '镜像由多层只读层组成，通过UnionFS合并', '回答准确', 85);