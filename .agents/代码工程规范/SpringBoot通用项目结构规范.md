# SpringBoot 通用项目结构规范

## 1. 文档目的

本文档用于约束 `server-api` 的后端分层、接口和基础能力。目录示例按实际项目包名 `com.example.web` 使用。

## 2. 通用技术栈约定

| 项目     | 推荐方案                               |
| -------- | -------------------------------------- |
| 后端框架 | SpringBoot                             |
| 数据库   | MySQL / PostgreSQL                     |
| ORM      | MyBatis-Plus                           |
| 鉴权方式 | Sa-Token                               |
| 接口风格 | GET / POST                             |
| 参数校验 | Jakarta Validation / Spring Validation |
| 金额字段 | `Double` |
| 接口文档 | Markdown                               |
| 文件上传 | 本地上传 / OSS / S3                    |
| 日志     | SLF4J + Logback                        |

## 3. 项目包名

本项目根包使用 `com.example.web`，新增模块沿用该包，不因模板示例改名。

## 4. 通用目录结构

```text
src/main/java/com/example/project
  ├─ ProjectApplication.java
  ├─ common
  │   ├─ result
  │   │   ├─ Result.java
  │   │   └─ ResultCode.java
  │   ├─ page
  │   │   └─ PageResult.java
  │   ├─ constant
  │   │   └─ CommonConstant.java
  │   └─ enums
  │       └─ BaseStatusEnum.java
  ├─ config
  │   ├─ CorsConfig.java
  │   ├─ WebMvcConfig.java
  │   ├─ SaTokenConfig.java
  │   ├─ SaTokenJwtConfig.java
  │   ├─ MybatisPlusConfig.java
  │   └─ JacksonConfig.java
  ├─ exception
  │   ├─ BusinessException.java
  │   └─ GlobalExceptionHandler.java
  ├─ security
  │   ├─ LoginAccountUtil.java
  │   ├─ PermissionCode.java
  │   └─ PermissionService.java
  ├─ module
  │   ├─ auth
  │   ├─ user
  │   ├─ system
  │   └─ demo
  └─ util
      ├─ DateUtil.java
      ├─ PasswordUtil.java
      └─ StringUtil.java
```

资源目录：

```text
src/main/resources
  └─ application.yml
```

## 5. 业务模块内部结构

每个业务模块统一使用下面结构。

```text
module/demo
  ├─ controller
  │   └─ DemoController.java
  ├─ service
  │   ├─ DemoService.java
  │   └─ impl
  │       └─ DemoServiceImpl.java
  ├─ mapper
  │   └─ DemoMapper.java
  ├─ entity
  │   └─ Demo.java
  ├─ dto
  │   ├─ DemoCreateDTO.java
  │   ├─ DemoUpdateDTO.java
  │   └─ DemoQueryDTO.java
  ├─ vo
  │   ├─ DemoListVO.java
  │   └─ DemoDetailVO.java
  └─ enums
      └─ DemoStatusEnum.java
```

如果模块很简单，可以先不创建 `enums`。

## 6. 分层职责

| 层                  | 职责                                           |
| ------------------- | ---------------------------------------------- |
| controller          | 接收请求、参数校验、调用 service、返回统一结果 |
| service             | 编写业务逻辑、状态判断、事务控制               |
| mapper | 负责数据库访问，不写业务判断                   |
| entity              | 和数据库表字段对应                             |
| dto                 | 接收前端请求参数                               |
| vo                  | 返回给前端展示的数据                           |
| enums               | 存放状态、类型等枚举                           |
| common              | 通用返回、分页、常量、基础枚举                 |
| exception           | 业务异常和全局异常处理                         |
| security            | Sa-Token 登录账号、权限码、权限加载            |
| config              | 框架配置                                       |
| util                | 无状态工具类                                   |

## 7. 命名规范

### 7.1 类命名

| 类型        | 命名规则                 | 示例              |
| ----------- | ------------------------ | ----------------- |
| Entity      | 业务名                   | `Demo`            |
| Mapper      | Entity + Mapper          | `DemoMapper`      |
| Service     | Entity + Service         | `DemoService`     |
| ServiceImpl | Entity + ServiceImpl     | `DemoServiceImpl` |
| Controller  | Entity + Controller      | `DemoController`  |
| 新增 DTO    | Entity + CreateDTO       | `DemoCreateDTO`   |
| 编辑 DTO    | Entity + UpdateDTO       | `DemoUpdateDTO`   |
| 查询 DTO    | Entity + QueryDTO        | `DemoQueryDTO`    |
| 列表 VO     | Entity + ListVO          | `DemoListVO`      |
| 详情 VO     | Entity + DetailVO        | `DemoDetailVO`    |
| 枚举        | Entity + 字段语义 + Enum | `DemoStatusEnum`  |

### 7.2 方法命名

| 操作      | 方法名示例         |
| --------- | ------------------ |
| 分页查询  | `pageDemos`        |
| 列表查询  | `listDemos`        |
| 详情查询  | `getDemoDetail`    |
| 新增      | `createDemo`       |
| 编辑      | `updateDemo`       |
| 删除      | `deleteDemo`       |
| 批量删除  | `batchDeleteDemos` |
| 启用/禁用 | `updateDemoStatus` |
| 导出      | `exportDemos`      |
| 导入      | `importDemos`      |

### 7.3 数据库命名

| 类型     | 规则             | 示例                     |
| -------- | ---------------- | ------------------------ |
| 表名     | 小写下划线       | `sys_user` / `biz_order` |
| 字段名   | 小写下划线       | `user_name`              |
| 主键     | 统一使用 `id`    | `id`                     |
| 创建时间 | `create_time`    | `create_time`            |
| 头像     | `avatar`         | `avatar`                 |
| 状态字段 | 语义 + `_status` | `audit_status`           |

- 基础字段和删除策略见第 18 节；`create_time` 由数据库使用 `CURRENT_TIMESTAMP` 自动写入。
- 状态、角色、类型等有限值字段遵守第 10 节的字符串枚举规则。

基础账号表约束：

- 账号表 `sys_account` 统一保存角色、账号、密码摘要、密码盐值、状态、最后登录时间等认证字段；角色和状态都使用字符串枚举表达。
- 管理员表 `sys_admin` 和用户表 `sys_user` 必须分开，两张资料表的 `id` 各自自增，并通过唯一的 `account_id` 外键与 `sys_account.id` 建立一对一关联。
- 管理员姓名、邮箱和头像保存在 `sys_admin`，用户昵称、邮箱和头像保存在 `sys_user`；`sys_account` 不保存昵称、邮箱和头像。
- 后续新增教师、商家、员工或其他账号角色时，必须新建该角色对应的独立资料表，不能把角色专属资料字段继续添加到 `sys_account`，也不能复用管理员表或用户表承载其他角色资料。
- 新角色资料表的 `id` 必须独立自增，并包含唯一且非空的 `account_id`；`account_id` 外键关联 `sys_account.id`，使用唯一索引保证一个账号在该角色资料表中最多只有一条记录。
- 新增角色时必须同步扩展账号角色枚举、角色资料 Entity、Mapper、Service、DTO/VO 和权限加载逻辑；统一登录仍使用 `/auth/login`，不能为新角色再创建重复的登录接口或重复保存密码。
- 创建新角色账号时，必须在同一事务内先创建账号记录，再使用生成的账号 ID 创建角色资料记录；任一步失败时整体回滚。
- 删除任意角色资料时，必须根据资料表中的 `account_id` 在同一事务内删除角色资料和关联账号；外键 `ON DELETE CASCADE` 只作为数据完整性兜底，不能代替 Service 层明确的删除业务逻辑。
- 登录只允许使用统一接口 `/auth/login`，后端根据 `sys_account.role` 自动识别账号角色，不能在管理员和用户 Service 中重复实现密码校验。
- 管理员和前台用户都必须有头像字段，字段名统一为 `avatar`。
- 头像字段保存通用上传接口返回的相对路径，不保存域名、IP、端口或本地绝对路径。
- 当前管理员 VO、当前用户 VO、管理员列表 VO、管理员详情 VO、用户列表 VO、用户详情 VO 都必须返回头像字段。
- 管理员和前台用户的新增、编辑 DTO 都应包含头像字段，后台页面应支持头像展示、上传和点击预览。
- 后台基础账号管理必须同时包含管理员管理和用户管理；两类账号各自走独立接口、独立 DTO/VO 和独立 Service 方法，不混用表结构。
- 管理员管理和用户管理都应支持分页查询、新增、编辑、硬删除、重置密码和启用禁用状态维护。
- 后台当前管理员必须提供个人资料修改和密码修改接口；个人资料修改只允许维护姓名、手机号、邮箱、头像等展示资料，不允许在当前账号资料接口中修改账号、状态或权限。
- 后台当前管理员修改密码必须校验原密码，重新生成盐值和密码摘要；接口只负责修改密码，前端成功后必须清理当前 Token 并跳转后台登录页重新登录。
- 前台当前用户必须提供个人资料修改和密码修改接口；个人资料修改只允许维护昵称、手机号、邮箱、头像等展示资料，不允许在当前账号资料接口中修改账号、状态或权限。
- 前台当前用户修改密码必须校验原密码，重新生成盐值和密码摘要；接口只负责修改密码，前端成功后必须清理当前 Token 并跳转前台登录页重新登录。

## 8. 通用接口路径规范

后台管理接口建议统一加：

```text
/admin
```

用户端接口建议统一加：

```text
/api
```

无需登录的公共接口可以使用：

```text
/public
```

示例：

```text
GET    /admin/users
POST   /admin/users
GET    /admin/users/{id}
POST   /admin/users/{id}
POST   /admin/users/{id}/delete
POST   /admin/users/{id}/reset-password

POST   /auth/login
GET    /api/auth/profile
POST   /api/auth/profile

GET    /public/captcha
```

接口方法约束：

- 后端控制器只使用 `@GetMapping` 和 `@PostMapping`。
- 查询、详情、公共读取接口使用 `GET`。
- 新增、编辑、删除、重置密码、状态变更、提交表单等写操作统一使用 `POST`。
- 不使用 `@PutMapping`、`@DeleteMapping`、`@PatchMapping`。
- 前端请求封装只调用 `request.get` 和 `request.post`，不调用 `request.put`、`request.delete`。

## 9. 接口返回规范

所有接口统一返回 `Result<T>`。

`T` 必须是明确类型：

- 查询详情、列表、登录、当前用户信息、系统信息等对前端展示的数据，必须使用 VO。
- 分页接口统一返回 `Result<PageResult<XXXListVO>>`。
- 写操作无返回数据时使用 `Result<Void>`。
- Controller 不允许直接返回 `Map<String, Object>`、`HashMap`、`JSONObject`、`Object` 或 Entity。
- Controller 不允许临时拼接返回字段，例如 `data.put("username", ...)`；字段组装必须放在 Service 或转换方法中，并返回明确 VO。
- Entity 不直接暴露给前端，避免密码、盐值等内部字段泄漏。

`Result` 固定包含：

| 字段      | 类型    | 说明                                        |
| --------- | ------- | ------------------------------------------- |
| `code`    | Integer | 业务状态码，成功为 `200`，失败通常为 `400`  |
| `status`  | Boolean | 请求处理状态，成功为 `true`，失败为 `false` |
| `message` | String  | 前端可展示提示，失败时必须返回具体可读错误  |
| `data`    | T       | 返回数据主体，失败时为 `null`               |

成功示例：

```json
{
  "code": 200,
  "status": true,
  "message": "success",
  "data": {}
}
```

失败示例：

```json
{
  "code": 400,
  "status": false,
  "message": "名称不能为空",
  "data": null
}
```

HTTP 状态码必须与业务结果一致，禁止所有响应统一返回 HTTP 200：

| 场景 | HTTP 状态码 | Result.code |
| --- | --- | --- |
| 成功 | 200 | 200 |
| 业务异常 / 参数校验失败 | 400 | 400 |
| 未登录或 Token 失效 | 401 | 401 |
| 已登录但无权限 | 403 | 403 |
| 数据库或服务端未捕获异常 | 500 | 500 |

分页返回建议：

```json
{
  "code": 200,
  "status": true,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 9.1 JSON 时间格式

后端必须提供 `config/JacksonConfig.java` 统一配置 JSON 时间格式。所有接口返回的 `LocalDateTime` 必须序列化为 `yyyy-MM-dd HH:mm:ss`，不能返回 `2026-08-31T18:23:41` 这种带 `T` 的 ISO-8601 格式。

```java
private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

@Bean
public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
    return builder -> {
        builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
    };
}
```

## 10. 枚举与状态字段规范

- 状态、角色、类型和开关等有限值统一使用枚举，枚举包含字符串 `code`、中文 `desc` 和按 code 查找的方法。
- 数据库存储有语义的字符串 code，例如 `ENABLED`、`DISABLED`、`PENDING`、`APPROVED`、`REJECTED`，不使用数字状态编码。
- Entity 优先使用对应枚举类型，使用 `@EnumValue` 映射数据库 code、`@JsonValue` 输出接口值；DTO 接收字符串时，Service 必须校验是否合法。
- 赋值、比较和默认值均引用枚举或其 code，不在 Service、Controller、Entity 中散落魔法字符串或状态常量。
- 通用枚举放在 `common/enums`，业务专属枚举放在所属模块的 `enums`。
- 每个枚举值的注释遵守 `代码注释规范.md`。

## 11. 数据权限规范

后端权限框架默认使用 Sa-Token，不再手写 Base64 Token、Session 工具或自定义 Token 解析器。

`pom.xml` 必须引入：

```xml
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>1.46.0</version>
</dependency>
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-jwt</artifactId>
    <version>1.46.0</version>
</dependency>
```

`application.yml` 必须提供 Sa-Token 基础配置，并支持前端通过 `Authorization: Bearer {token}` 传递 Token：

```yaml
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: ${SA_TOKEN_TIMEOUT:7200}
  active-timeout: ${SA_TOKEN_ACTIVE_TIMEOUT:1800}
  is-concurrent: true
  is-share: false
  is-read-body: false
  is-read-cookie: false
  is-read-header: true
```

JWT 密钥不允许写入仓库。必须优先读取 `JWT_SECRET` 环境变量；未配置时由后端生成随机密钥并保存到用户目录下的本地文件（例如 `~/.forgestack/jwt-secret`），生产环境必须显式配置环境变量。

使用 Sa-Token JWT 简单模式，Token 本身仍是 JWT，同时保留服务端会话，因此退出登录、踢人下线和禁用账号可以立即生效：

```java
@Bean
public StpLogic getStpLogicJwt() {
    return new StpLogicJwtForSimple();
}
```

退出登录必须调用 `POST /auth/logout` 并执行 `StpUtil.logout()`，前端在成功后清理本地 Token。权限加载时必须同时校验账号 `status`，不能只校验角色。

`config/SaTokenConfig.java` 必须注册 Sa-Token 拦截器，并显式开启注解鉴权：

```java
registry.addInterceptor(new SaInterceptor(handle -> {
    SaRouter.match(SaHttpMethod.OPTIONS).back();
    SaRouter.match("/admin/**")
            .check(r -> StpUtil.checkLogin());
    SaRouter.match("/api/auth/**")
            .check(r -> StpUtil.checkLogin());
    SaRouter.match("/api/files/**")
            .check(r -> StpUtil.checkLogin());
}).isAnnotation(true)).addPathPatterns("/**")
        .excludePathPatterns("/public/**", "/files/**");
```

浏览器跨域预检请求 `OPTIONS` 必须在 Sa-Token 拦截器最前面放行，不能要求登录，否则前台和后台会出现 CORS 预检失败。

CORS 不允许使用 `allowedOriginPatterns("*")` 与 `allowCredentials(true)` 的组合。默认只放行本机开发来源，例如 `http://localhost:*`、`http://127.0.0.1:*`；生产环境通过 `CORS_ALLOWED_ORIGIN_PATTERNS` 环境变量配置明确来源。

权限码统一放在 `security/PermissionCode.java`。受保护的 Controller 方法必须使用 `@SaCheckPermission(PermissionCode.XXX)` 定义权限码。

### 11.1 账号身份与权限加载

基础账号权限示例：

```text
admin:profile
admin:profile:update
admin:password:update
admin:file:upload
admin:list
admin:detail
admin:create
admin:update
admin:delete
admin:reset-password
user:list
user:detail
user:create
user:update
user:delete
user:reset-password
web:profile
web:profile:update
web:password:update
web:file:upload
```

`security/PermissionService.java` 实现 `StpInterface` 加载权限。基础脚手架阶段可以按端侧返回固定权限码；后续接入角色权限表时，再改为通过 MyBatis-Plus 查询角色和权限，不允许手写 SQL。

登录成功后使用 Sa-Token 颁发 Token：

```java
LoginAccountUtil.login(account.getId());
return new LoginVO(LoginAccountUtil.getTokenValue(), account.getRole().getClientType(), account.getUsername());
```

Sa-Token 的 `loginId` 不允许包含冒号 `:`，统一账号登录 ID 使用安全前缀，例如 `account_1`。账号角色通过 `sys_account.role` 查询，权限码可以继续使用 `admin:profile`、`web:profile` 这种冒号命名。

当前登录账号统一从 Sa-Token 上下文获取，不要在 Controller 中接收并解析 `Authorization`：

```java
Long accountId = LoginAccountUtil.getCurrentAccountId();
```

### 11.2 后台管理端

- 后台接口必须校验后台登录身份。
- 后台接口路径建议统一使用 `/admin/**`。
- 需要多角色时，通过角色、权限、菜单控制访问范围。
- `/auth/login` 是管理员和用户共用的公开登录接口，其余 `/admin/**` 默认必须登录并校验管理员权限。

### 11.3 用户端

- 用户端接口必须从 Token 或 Session 中获取当前用户身份。
- 不允许直接相信前端传入的 `userId`。
- 用户只能访问自己名下的数据。
- `/auth/login` 是管理员和用户共用的公开登录接口；`/api/auth/profile`、`/api/auth/password`、`/api/files/**` 等用户个人接口必须登录。

### 11.4 公共接口

- 公共接口只返回允许公开的数据。
- 公共接口不要返回敏感字段。
- `/public/**` 和 `/files/**` 默认放行。

### 11.5 找回密码与注销

- 找回密码必须先发送邮箱验证码，验证码只能通过用户资料表中的邮箱接收；禁止仅凭“账号 + 邮箱”直接签发重置凭证。
- 验证码必须设置有效期、发送冷却时间和最大错误次数，验证成功后立即失效。
- 开发环境可以将验证码输出到后端日志；生产环境必须接入真实邮件发送服务。
- 重置密码成功后必须注销该账号的全部登录态。
- 修改密码、重置密码、禁用账号、删除账号后必须清理对应账号的登录态。
- 退出登录必须提供 `POST /auth/logout`，前端成功提示后清理本地 Token 并跳转登录页。

## 12. 事务规范

以下情况必须使用事务：

- 一次操作写入多张表。
- 一次操作既更新主表又更新明细表。
- 涉及库存、余额、积分、状态流转等一致性问题。
- 涉及审核、支付、取消、退款、审批等关键业务动作。

推荐写法：

```java
@Transactional(rollbackFor = Exception.class)
```

## 13. 参数校验规范

DTO 中使用注解做基础校验。

示例：

```java
@NotBlank(message = "名称不能为空")
private String name;

@NotNull(message = "金额不能为空")
private Double amount;
```

Controller 中使用：

```java
public Result<Void> create(@Valid @RequestBody DemoCreateDTO dto)
```

## 14. 异常处理规范

业务异常统一抛出 `BusinessException`。

示例：

```java
throw new BusinessException("数据不存在");
```

全局异常处理统一返回 `Result`，并同步设置正确的 HTTP 状态码，不允许直接把 Java 异常堆栈返回给前端。

强制要求：

- `BusinessException` 返回 HTTP 400，并返回业务异常自身 message。
- `NotLoginException` 返回 HTTP 401，message 为 `登录已失效，请重新登录`。
- `NotPermissionException` 返回 HTTP 403，message 为 `没有权限访问：{permission}`。
- `MethodArgumentNotValidException`、`BindException` 等参数校验异常返回 HTTP 400，优先返回 DTO 校验注解中的中文提示，例如 `名称不能为空`。
- `ConstraintViolationException` 返回 HTTP 400；`HttpMessageNotReadableException` 返回 HTTP 400 和 `请求参数格式不正确`。
- `MyBatisSystemException` 返回 HTTP 500；如果底层异常信息包含 `Failed to obtain JDBC`，返回明确的数据库连接排查提示，例如 `数据库没有连接上，请检查数据库账号和密码是否正确、数据库是否启动、数据库是否正常运行、数据库配置是否正确`。
- 其他未捕获异常返回 HTTP 500 和通用提示 `服务端异常，请查看后端日志`，完整异常必须通过后端日志记录。
- 不允许向前端返回完整堆栈、源码路径、SQL 语句、数据库账号密码等敏感信息。

## 15. Mapper 规范

- 后端 ORM 固定使用 MyBatis-Plus，不再使用普通 MyBatis starter 或 JPA 作为默认方案。
- `pom.xml` 使用 `mybatis-plus-spring-boot3-starter`。
- `application.yml` 使用 `mybatis-plus:` 配置，不使用旧的 `mybatis:` 根配置。
- `mapper-locations` 可以保留为框架配置项；默认不创建 Mapper XML 文件。
- `type-aliases-package` 必须按当前系统包名配置，本项目使用 `com.example.web.module.*.entity`。
- Mapper 接口继承 `BaseMapper<Entity>`，简单 CRUD 优先使用 MyBatis-Plus 通用方法。
- Entity 按表名配置 `@TableName`，自增主键配置 `@TableId(type = IdType.AUTO)`。
- 未经用户明确允许，不允许手写 SQL。禁止在 Mapper 中使用 `@Select`、`@Insert`、`@Update`、`@Delete`，禁止编写 XML SQL，禁止在代码中拼接 SQL 片段。
- 条件查询、分页、更新、硬删除统一使用 MyBatis-Plus 的 `BaseMapper`、`LambdaQueryWrapper`、`LambdaUpdateWrapper`、`Page`、`IService` 等能力。
- 登录时间等字段在 Java 中使用 `LocalDateTime.now()` 设置，不使用 `setSql("last_login_time = now()")` 这类 SQL 片段。
- 不使用 `.last(...)` 拼接分页或限制条件；分页必须使用 MyBatis-Plus 分页插件。
- 不使用 `setSql(...)`、`apply(...)`、`inSql(...)`、`exists(...)` 等直接注入 SQL 的 Wrapper 方法，除非用户单独批准当前场景。

示例：

```text
DemoMapper.java 继承 BaseMapper<Demo>
```

## 16. Controller 规范

Controller 只做三件事：

1. 接收参数。
2. 调用 Service。
3. 返回 Result。

Controller 是接口入口，不是业务实现层。任何业务判断、数据库访问、鉴权解析、状态判断、密码校验和返回字段组装都必须下沉到 Service 或专门组件中。

强制要求：

- 返回类型遵守第 9 节，字段定义遵守 DTO / VO 规范。
- Controller 不直接操作 Mapper，不直接注入 Mapper，不直接调用 `selectById`、`selectOne`、`selectPage`、`insert`、`update`、`delete` 等数据库访问方法。
- Controller 不直接写 `LambdaQueryWrapper`、`LambdaUpdateWrapper`、`QueryWrapper`、`UpdateWrapper` 等数据库查询或更新条件。
- Controller 不直接做 Token 解析、登录态校验、账号存在性校验、账号状态判断、密码校验、权限判断、数据归属判断、状态流转判断。
- Controller 可以做的参数处理只限于路径参数赋值、请求体接收、`@Valid` 参数校验和调用 Service 前的极轻量参数补齐，例如把 `@PathVariable id` 写回 DTO。
- Controller 方法体通常不超过 5 行；超过时必须优先检查是否把业务逻辑写进了 Controller。

禁止示例：

```java
@GetMapping("/profile")
public Result<AdminProfileVO> profile() {
    Long accountId = LoginAccountUtil.getCurrentAccountId();
    Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getAccountId, accountId));
    if (admin == null || !EnableStatusEnum.ENABLED.getCode().equals(admin.getStatus())) {
        throw new BusinessException("登录已失效");
    }
    return Result.success(toProfileVO(admin));
}
```

推荐示例：

```java
@GetMapping("/profile")
@SaCheckPermission(PermissionCode.ADMIN_PROFILE)
public Result<AdminProfileVO> profile() {
    return Result.success(adminService.getCurrentAdminProfile());
}
```

## 17. Service 规范

Service 负责业务规则和事务控制。

常见职责：

- 判断数据是否存在。
- 判断当前状态是否允许操作。
- 校验当前用户是否有权限。
- 通过 Sa-Token 登录态查询当前用户或管理员信息。
- 校验账号状态、密码正确性、数据归属和状态流转合法性。
- 组装 VO，控制哪些字段可以返回给前端。
- 处理新增、编辑、删除、审核、状态流转等业务逻辑。
- 调用 Mapper 操作数据库。

## 18. Entity 规范

Entity 只映射数据库字段，不放复杂业务逻辑。

基础字段建议统一：

```java
private Long id;
private LocalDateTime createTime;
```

所有表必须包含 `id`、`create_time`，对应 Java 字段 `id`、`createTime`；不添加 `create_by`、`creator`、`update_time`、`update_by`、`deleted`。

删除规范：

- 默认使用硬删除。
- 不使用 `@TableLogic`。
- 删除操作使用 MyBatis-Plus 的 `deleteById` 或 `delete` 方法。
- 只有用户明确要求保留删除记录或审计追踪时，才单独设计软删除字段。

## 19. DTO / VO 规范

### DTO

DTO 用于接收请求参数，不直接把 Entity 暴露给前端。

常见 DTO：

```text
CreateDTO
UpdateDTO
QueryDTO
LoginDTO
RegisterDTO
AuditDTO
StatusDTO
```

### VO

VO 用于返回前端展示。

返回类型和禁止暴露的结构见第 9 节。

`ListVO`、`DetailVO`、`OptionVO`、`TreeVO`、`StatisticsVO` 等 VO 必须各自独立定义字段，不允许通过 `DetailVO extends ListVO` 或其他 VO 继承方式复用字段。

如果列表和详情返回字段重复，由 Service 转换方法分别显式赋值，例如 `fillListVO(ListVO vo, ...)` 和 `fillDetailVO(DetailVO vo, ...)` 各自维护对应字段，避免接口返回边界被继承关系隐藏。

常见 VO：

```text
ListVO
DetailVO
LoginVO
OptionVO
TreeVO
StatisticsVO
```

## 20. 文件上传规范

第一版项目可以使用本地上传。

上传目录示例：

```text
external-resources/{module}/{fileType}/{yyyyMMdd}
```

规则：

- 文件上传配置写入唯一资源配置文件 `application.yml`：

```yaml
spring:
  servlet:
    multipart:
      max-request-size: ${FILE_MAX_REQUEST_SIZE:20MB}
      max-file-size: ${FILE_MAX_SIZE:10MB}

app:
  file:
    upload-dir: external-resources
    access-prefix: /files
    max-size: ${FILE_MAX_SIZE_BYTES:10485760}
    allowed-extensions: ${FILE_ALLOWED_EXTENSIONS:jpg,jpeg,png,gif,webp,bmp,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,rar,txt}
```

- 本地上传文件默认保存到项目运行目录下的 `external-resources/`，上传目录不得提交到版本库。
- Web MVC 必须配置静态资源映射，例如把 `/files/**` 映射到 `file:external-resources/`，保证上传后返回的 URL 可以直接访问。
- 上传接口只使用 `POST`。
- 必须在 Service 层同时校验文件大小和扩展名，不能只依赖前端 `accept`。
- 扩展名必须使用白名单，禁止上传 `html`、`htm`、`svg`、`js`、`mjs`、`jsp`、`php` 等可执行或可被浏览器解释为脚本的文件。
- 前端页面即使只允许图片，后端也必须保留同样的白名单校验。
- 上传接口返回相对 URL，例如 `/files/common/image/20260831/xxx.png`。
- 业务表只保存上传接口返回的相对 URL 或文件标识，不保存域名、端口、Base64 大文本或本地绝对路径；前端封装按各端分层规范执行。
- 不要把大文件直接存进数据库字段。
- 文件上传能力必须封装为通用模块，例如 `module/file`，不要只写在商品、公告、轮播图等单个业务模块里。
- 不允许出现只服务单一业务的上传接口作为最终交付，例如 `product/upload`、`banner/upload`；如确有业务差异，通过通用接口参数或通用 Service 扩展处理。
- 后台文件上传接口建议统一为 `POST /admin/files/upload`，通过 `module` 或 `fileType` 参数区分商品图、公告图、轮播图、头像等业务场景。
- 前台文件上传接口建议统一为 `POST /api/files/upload`，通过 `module` 或 `fileType` 参数区分头像、附件等前台上传场景。
- 文件上传接口创建后，如果页面存在上传控件，必须接真实上传接口完成浏览器自检，不允许用“模拟上传”“内置演示图”作为最终交付。

## 21. 日志规范

关键业务操作建议写操作日志：

- 新增重要数据。
- 编辑重要数据。
- 删除重要数据。
- 审核操作。
- 状态变更。
- 导入导出。
- 系统配置修改。

## 22. 测试规范

优先保证核心 Service 的测试。

建议测试：

- 新增成功。
- 编辑成功。
- 删除成功。
- 参数为空。
- 数据不存在。
- 状态不允许操作。
- 无权限访问。
