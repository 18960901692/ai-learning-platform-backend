# AI 伴学平台后端服务

本目录是 AI 伴学平台的后端服务，基于 Spring Boot 3 构建，负责用户认证、技能体系、AI 对话、考核测评、学习记录、学习路径、成长报告、知识库（RAG）、Dify 集成和管理后台接口。

## 技术栈

- Java 17
- Spring Boot 3.2.5
- MyBatis-Plus 3.5.5
- MySQL 8.0
- Redis 6.x+（缓存 + RAG 向量库）
- JJWT 0.12.3 + spring-security-crypto（BCrypt 密码加密）
- Spring AI 1.0.0（OpenAI 兼容接口 + Redis Vector Store + Tika Document Reader）
- Spring AI Alibaba 1.0.0.2（阿里云 DashScope Embedding）
- Springdoc OpenAPI 2.3.0
- Apache HttpClient 5
- Lombok
- Validation
- Spring AOP

## 目录结构

```text
ai-learning-platform-backend/
├── src/main/java/com/aicompanion/
│   ├── aspect/         # AOP 切面（AiCallLogAspect，AI 调用日志）
│   ├── common/         # 通用模块
│   │   ├── ai/         # RedisChatMemory（对话记忆持久化）
│   │   ├── exception/  # BusinessException、GlobalExceptionHandler
│   │   ├── heartbeat/  # LearningHeartbeatBuffer（学习心跳缓冲）
│   │   ├── response/   # Result<T>、PageResult<T>
│   │   └── util/       # JwtUtil、SecurityUtil、ScoreExtractor、TokenBlacklistService
│   ├── config/         # Spring、AI、Redis、MyBatis、Web、Dify、RAG 配置
│   ├── controller/     # REST API 控制器（18 个）
│   ├── interceptor/    # JwtInterceptor（JWT 拦截器）
│   ├── mapper/         # MyBatis-Plus Mapper
│   ├── model/          # 数据模型
│   │   ├── dto/        # DTO（接收前端参数，带 @Valid 校验）
│   │   ├── entity/     # 实体类（对应数据库表，继承 BaseEntity）
│   │   └── vo/         # VO（返回给前端，不含敏感字段）
│   ├── service/        # 业务接口与实现
│   │   └── impl/       # Service 实现类
│   ├── task/           # 定时任务（考核清理、心跳刷新）
│   ├── tool/           # Spring AI Tools（技能查询、用户技能分析、学习记录）
│   └── AiLearningPlatformApplication.java
└── src/main/resources/
    ├── application.yml # 应用配置（敏感配置通过环境变量覆盖）
    ├── schema.sql      # 数据库建表脚本
    ├── mapper/         # XML Mapper
    └── sql/            # 补充 SQL 脚本
```

## 核心功能

| 模块 | 说明 |
|---|---|
| 认证模块 | 用户注册、登录、刷新 Token、退出登录、管理员登录 |
| 用户模块 | 当前用户信息、学习统计、用户管理、学习画像、头像上传 |
| 技能模块 | 技能树、技能分类、技能管理、用户技能掌握状态 |
| AI 对话模块 | 同步对话、SSE 流式对话、AI 面试、知识点生成、会话管理 |
| RAG 知识库模块 | 文件上传（PDF/Word/HTML/TXT）、向量化存储、检索增强生成 |
| Dify 集成 | Dify 对话、简历分析、考核阅卷结果保存 |
| 考核模块 | 创建考核会话、获取考核结果、保存评分 |
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
| `spring.data.redis` | Redis 连接配置（缓存 + 向量库共用） |
| `spring.ai.openai` | Spring AI OpenAI 兼容接口配置（对话模型） |
| `spring.ai.dashscope` | 阿里云 DashScope 配置（Embedding 模型） |
| `spring.ai.vectorstore` | RAG 向量库配置（Redis Vector Store） |
| `jwt` | JWT 密钥和 Token 有效期配置 |
| `dify` | Dify API 地址和密钥配置 |
| `file.upload.path` | 文件上传目录 |
| `springdoc` | Swagger / OpenAPI 文档配置 |

建议使用环境变量覆盖敏感配置：

```bash
set AI_API_KEY=你的模型APIKey
set AI_BASE_URL=https://api.deepseek.com
set AI_MODEL=deepseek-chat
set DASHSCOPE_API_KEY=你的阿里云百炼APIKey
set DIFY_API_KEY=你的DifyAPIKey
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

| 分组 | 路径前缀 | Controller | 说明 |
|---|---|---|---|
| 用户认证 | `/auth` | AuthController | 注册、登录、刷新 Token、退出登录 |
| 管理员认证 | `/admin/auth` | AdminAuthController | 管理员登录、刷新 Token、退出登录 |
| 用户 | `/users` | UserController、FileController | 当前用户、用户管理、头像上传、学习画像 |
| 技能 | `/skills` | SkillController | 技能树、技能查询、技能管理 |
| AI 聊天 | `/ai/chat` | AiChatController | 聊天、流式聊天、AI 面试、知识点生成 |
| AI 会话 | `/ai/chat/session` | ChatSessionController | 会话管理 |
| RAG 知识库 | `/rag` | RagController | 文件上传、检索增强生成 |
| Dify 集成 | `/dify` | DifyController | Dify 对话、简历分析 |
| 考核 | `/exam` | ExamController | 开始考核、查询结果、保存评分 |
| 学习 | `/learning` | LearningRecordController | 开始学习、心跳、结束学习 |
| 学习路径 | `/learning-path` | LearningPathController | 学习路径推荐 |
| 成长报告 | `/report` | GrowthReportController | 学习统计、AI 成长分析 |
| 管理仪表盘 | `/admin/dashboard` | AdminDashboardController | 仪表盘统计 |
| 管理 AI 调用 | `/admin/ai-call` | AiCallController | AI 调用统计 |
| 管理聊天记录 | `/admin/chat-sessions` | AdminChatController | 聊天会话管理 |
| 管理学习记录 | `/admin/learning-records` | AdminLearningRecordController | 学习记录管理 |
| 管理考核记录 | `/admin/exam-records` | AdminExamRecordController | 考核记录管理 |

## AI 能力

### Spring AI 对话
后端通过 Spring AI 接入 OpenAI 兼容模型（默认 DeepSeek），用于 AI 伴学聊天、SSE 流式对话、AI 模拟面试、技能知识点生成、学习路径推荐理由和成长报告 AI 分析。

### Spring AI Tools
后端提供 3 个 Spring AI Tools，让模型可以主动调用：
- `SkillLookupTool`：查询技能信息
- `UserSkillAnalysisTool`：分析用户技能掌握状态
- `LearningRecordTool`：查询学习记录

### RAG 检索增强生成
使用 Spring AI 构建 RAG 流水线：
- **文档读取**：Tika Document Reader 支持 PDF/Word/HTML/TXT
- **向量化**：阿里云 DashScope Embedding
- **向量存储**：Redis Vector Store（持久化，重启不丢失）
- **检索增强**：QuestionAnswerAdvisor 将知识库内容注入 Prompt

### Dify 集成
后端集成 Dify API，提供 Dify 对话、简历分析能力，并负责保存 Dify 返回的考核评分结果。

## 数据表

核心数据表定义位于 `src/main/resources/schema.sql`，包括：

| 表名 | 说明 |
|---|---|
| `user` | 用户表 |
| `skill` | 技能表 |
| `user_skill` | 用户技能掌握状态 |
| `learning_plan` | 学习计划 |
| `learning_plan_skill` | 学习计划-技能关联 |
| `learning_record` | 学习记录 |
| `chat_session` | AI 对话会话 |
| `chat_message` | AI 对话消息 |
| `exam_session` | 考核会话 |
| `ai_call_log` | AI 调用日志 |
| `knowledge_file` | 知识库文件（RAG） |

## 常用命令

```bash
mvn clean package
mvn spring-boot:run
mvn test
```
