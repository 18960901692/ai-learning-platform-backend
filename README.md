# AI 伴学平台后端服务

本目录是 AI 伴学平台的后端服务，基于 Spring Boot 3 构建，负责用户认证、技能体系、AI 对话、考核测评、学习记录、学习路径、成长报告和管理后台接口。

## 技术栈

- Java 17
- Spring Boot 3.2.5
- MyBatis-Plus 3.5.5
- MySQL
- Redis
- JWT
- Spring AI OpenAI
- Springdoc OpenAPI
- Lombok
- Validation
- Spring AOP

## 目录结构

```text
ai-learning-platform-backend/
├── src/main/java/com/aicompanion/
│   ├── aspect/         # AOP 切面，如 AI 调用日志
│   ├── common/         # 通用响应、异常、工具类、Redis 记忆
│   ├── config/         # Spring、AI、Redis、MyBatis、Web 配置
│   ├── controller/     # REST API 控制器
│   ├── interceptor/    # JWT 拦截器
│   ├── mapper/         # MyBatis-Plus Mapper
│   ├── model/          # DTO、Entity、VO
│   ├── service/        # 业务接口与实现
│   ├── task/           # 定时任务
│   ├── tool/           # Spring AI Tools
│   └── AiLearningPlatformApplication.java
└── src/main/resources/
    ├── application.yml # 应用配置
    ├── schema.sql      # 数据库建表脚本
    └── mapper/         # XML Mapper
```

## 核心功能

| 模块 | 说明 |
|---|---|
| 认证模块 | 用户注册、登录、刷新 Token、退出登录、管理员登录 |
| 用户模块 | 当前用户信息、学习统计、用户管理、学习画像、头像上传 |
| 技能模块 | 技能树、技能分类、技能管理、用户技能掌握状态 |
| AI 对话模块 | 同步对话、流式对话、AI 面试、知识点生成、会话管理 |
| 考核模块 | 创建考核会话、获取考核结果、保存 Dify 阅卷结果 |
| 学习模块 | 开始学习、学习心跳、结束学习、学习记录统计 |
| 学习路径模块 | 基于规则和 AI 生成个性化学习路径 |
| 成长报告模块 | 统计学习数据并生成 AI 成长分析 |
| 管理后台模块 | 仪表盘、用户、技能、学习记录、考核记录、AI 调用统计、聊天记录 |

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Redis 6.x+

## 数据库初始化

创建数据库：

```sql
CREATE DATABASE ai_learning_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

导入建表脚本：

```bash
mysql -u root -p ai_learning_platform < src/main/resources/schema.sql
```

## 配置说明

配置文件路径：`src/main/resources/application.yml`

| 配置 | 说明 |
|---|---|
| `server.port` | 服务端口，默认 `8080` |
| `server.servlet.context-path` | API 上下文路径，默认 `/api` |
| `spring.datasource` | MySQL 数据库连接配置 |
| `spring.data.redis` | Redis 连接配置 |
| `spring.ai.openai` | Spring AI OpenAI 兼容接口配置 |
| `jwt` | JWT 密钥和 Token 有效期配置 |
| `file.upload.path` | 文件上传目录 |
| `springdoc` | Swagger / OpenAPI 文档配置 |

建议使用环境变量覆盖敏感配置：

```bash
set AI_API_KEY=你的模型APIKey
set AI_BASE_URL=https://api.deepseek.com
set AI_MODEL=deepseek-chat
set REDIS_HOST=localhost
set REDIS_PORT=6379
```

生产环境请不要将数据库密码、JWT 密钥、AI Key 等敏感信息提交到代码仓库。

## 启动服务

```bash
mvn spring-boot:run
```

服务启动后访问：`http://localhost:8080/api`

Swagger 文档：`http://localhost:8080/api/swagger-ui.html`

OpenAPI JSON：`http://localhost:8080/api/v3/api-docs`

## 接口约定

- API 基础路径：`/api`
- 统一响应格式：`Result<T>`
- 成功响应示例：`{ "code": 200, "message": "success", "data": ... }`
- 认证方式：JWT Bearer Token
- 请求头：`Authorization: Bearer <access_token>`

## 主要接口分组

| 分组 | 路径前缀 | 说明 |
|---|---|---|
| 用户认证 | `/auth` | 注册、登录、刷新 Token、退出登录 |
| 管理员认证 | `/admin/auth` | 管理员登录、刷新 Token、退出登录 |
| 用户 | `/users` | 当前用户、用户管理、头像上传、学习画像 |
| 技能 | `/skills` | 技能树、技能查询、技能管理 |
| AI 聊天 | `/ai/chat` | 聊天、流式聊天、AI 面试、知识点、会话管理 |
| 考核 | `/exam` | 开始考核、查询结果、保存评分 |
| 学习 | `/learning` | 开始学习、心跳、结束学习 |
| 学习路径 | `/learning-path` | 学习路径推荐 |
| 成长报告 | `/report` | 学习统计、AI 成长分析 |
| 管理后台 | `/admin` | 仪表盘、记录查询、AI 统计、会话管理 |

## AI 能力

后端通过 Spring AI 接入 OpenAI 兼容模型，默认用于 AI 伴学聊天、SSE 流式对话、AI 模拟面试、技能知识点生成、学习路径推荐理由和成长报告 AI 分析。后端同时提供 Spring AI Tools，用于让模型查询技能信息、用户技能分析和学习记录。Dify 主要由客户端调用，后端负责保存 Dify 返回的考核评分结果。

## 数据表

核心数据表定义位于 `src/main/resources/schema.sql`，包括 `user`、`skill`、`user_skill`、`learning_plan`、`learning_plan_skill`、`learning_record`、`chat_session`、`chat_message`、`exam_session`、`ai_call_log`。

## 常用命令

```bash
mvn clean package
mvn spring-boot:run
mvn test
```
