-- AI Learning Platform 建表脚本
-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS ai_learning_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_learning_platform;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id                       BIGINT       NOT NULL        COMMENT '主键ID（雪花算法）',
    username                 VARCHAR(50)  NOT NULL        COMMENT '用户名',
    password                 VARCHAR(255) NOT NULL        COMMENT '密码（BCrypt 加密）',
    nickname                 VARCHAR(50)  DEFAULT NULL    COMMENT '昵称',
    email                    VARCHAR(100) DEFAULT NULL    COMMENT '邮箱',
    phone                    VARCHAR(20)  DEFAULT NULL    COMMENT '手机号',
    avatar                   VARCHAR(500) DEFAULT NULL    COMMENT '头像URL',
    profession               VARCHAR(100) DEFAULT NULL    COMMENT '职业/专业',
    bio                      VARCHAR(500) DEFAULT NULL    COMMENT '个人简介',
    role                     VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色：STUDENT/TEACHER/ADMIN',
    status                   TINYINT      NOT NULL DEFAULT 1          COMMENT '状态：0-禁用 1-正常',
    refresh_token            VARCHAR(100) DEFAULT NULL    COMMENT '刷新令牌（记住密码）',
    refresh_token_expire_time DATETIME    DEFAULT NULL    COMMENT '刷新令牌过期时间',
    deleted                  TINYINT      NOT NULL DEFAULT 0          COMMENT '逻辑删除：0-未删除 1-已删除',
    create_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_refresh_token (refresh_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- 技能树表
CREATE TABLE IF NOT EXISTS skill_tree (
    id                       BIGINT       NOT NULL        COMMENT '主键ID（雪花算法）',
    name                     VARCHAR(100) NOT NULL        COMMENT '技能名称',
    category                 VARCHAR(50)  NOT NULL        COMMENT '分类：FRONTEND/BACKEND/TOOL/BASIC',
    description              VARCHAR(500) DEFAULT NULL    COMMENT '技能描述',
    status                   TINYINT      NOT NULL DEFAULT 1          COMMENT '状态：0-禁用 1-启用',
    create_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='技能树表';
