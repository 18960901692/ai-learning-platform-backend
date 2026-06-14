# AI 伴学平台 - 后端服务

## 技术栈
- 语言：Java 17
- 框架：Spring Boot 3.2（单体架构）
- ORM：MyBatis-Plus 3.5
- 数据库：MySQL 8.0
- 安全：Spring Security + JWT
- API 文档：Swagger (OpenAPI 3.0)

## 分层架构
backend/src/main/java/com/aicompanion/
├── model/                  # 数据模型层
│   ├── entity/             # 实体类（对应数据库表，继承 BaseEntity）
│   ├── dto/                # DTO（接收前端参数，带 @Valid 校验）
│   ├── vo/                 # VO（返回给前端，不含敏感字段）
│   └── query/              # 查询参数对象
├── mapper/                 # 数据访问层（继承 BaseMapper<T>）
├── service/                # 业务逻辑层
│   ├── XxxService.java     # 接口
│   └── impl/
│       └── XxxServiceImpl.java  # 实现
├── controller/             # 控制器层（接收 HTTP 请求）
├── common/                 # 公共模块
│   ├── response/           # Result<T>、PageResult<T>
│   ├── exception/          # BusinessException、GlobalExceptionHandler
│   └── util/               # JwtUtil、SecurityUtil
└── config/                 # 配置类

## 编码规范
- Controller 只做请求接收和响应返回，不写业务逻辑
- 业务逻辑写在 Service 层
- 统一返回 Result<T>，不直接返回 Entity
- 使用 DTO 接收参数，使用 VO 返回数据
- 使用 @RequiredArgsConstructor 构造器注入，不使用 @Autowired
- 使用 Lombok 的 @Data 简化代码
- 接口必须添加 Swagger 注解（@Tag、@Operation）
- 参数必须使用 @Valid 校验

## 约束条件
- 禁止在 Controller 中直接调用 Mapper
- 禁止使用 @Autowired，使用构造器注入
- 禁止返回 Entity 给前端，必须转换为 VO
- 禁止使用 System.out.println，使用 @Slf4j
- 数据库操作通过 MyBatis-Plus，禁止手写 SQL（复杂查询除外）
- 禁止修改 BaseEntity 的结构
- 所有新增接口必须先在 Swagger (项目文档) 中能正常调通

## 新增模块步骤
1. model/entity/ 创建实体类，继承 BaseEntity
2. model/dto/ 创建 DTO 类，添加 @Valid 校验
3. model/vo/ 创建 VO 类
4. mapper/ 创建 Mapper 接口，继承 BaseMapper
5. service/ 创建 Service 接口
6. service/impl/ 创建 Service 实现
7. controller/ 创建 Controller

## 参考文件
- Controller：controller/AuthController.java（认证）、controller/UserController.java（用户管理）
- Service：service/impl/UserServiceImpl.java
- Entity：model/entity/User.java
- DTO：model/dto/LoginDTO.java、model/dto/RegisterDTO.java
- VO：model/vo/UserVO.java、model/vo/LoginVO.java