---
# 项目结构文档

> 社区生鲜配送系统 — 完整项目文件索引与功能说明  
> 更新日期：2026-06-01

---

## 一级目录

| 路径 | 说明 |
|------|------|
| `community-fresh-delivery/` | Spring Boot 后端工程根目录 |
| `function.md` | 需求文档（业务目标 + 技术目标 + 订单状态机 + 分阶段规划） |
| `devlog.md` | 开发日志（按日记录 + 当前进度 + 待办清单） |
| `project-structure.md` | 项目结构文档（本文件 — 逐文件功能说明） |

---

## 根目录文件

### `pom.xml`
Maven 项目描述文件。定义坐标 `com.duyell:community-fresh-delivery:0.0.1-SNAPSHOT`，继承 `spring-boot-starter-parent:4.0.6`（Java 21）。

**依赖清单：**
| 依赖 | 版本 | 用途 |
|------|------|------|
| `spring-boot-starter-webmvc` | 4.0.6 | REST 接口 + 内嵌 Tomcat |
| `spring-boot-starter-security` | 4.0.6 | 认证鉴权框架 |
| `spring-boot-starter-data-redis` | 4.0.6 | Redis 缓存 / 分布式锁 |
| `spring-boot-starter-amqp` | 4.0.6 | RabbitMQ 消息队列 |
| `spring-boot-starter-validation` | 4.0.6 | Bean Validation 参数校验 |
| `mybatis-plus-spring-boot3-starter` | 3.5.9 | ORM 框架（含自动填充 / 逻辑删除） |
| `mybatis-plus-jsqlparser` | 3.5.9 | 分页 SQL 解析（3.5.9 起独立模块） |
| `mysql-connector-j` | runtime | MySQL 驱动 |
| `knife4j-openapi3-jakarta-spring-boot-starter` | 4.5.0 | Swagger 接口文档 UI |
| `jjwt-api / jjwt-impl / jjwt-jackson` | 0.12.6 | JWT 令牌签发 / 解析 |
| `aliyun-sdk-oss` | 3.18.1 | 阿里云 OSS 文件上传 |
| `hutool-all` | 5.8.30 | 通用工具库 |
| `lombok` | optional | 简化 getter/setter/构造器 |

> 仅列生产依赖，test 依赖（含 `-test` 后缀的 starter）略。

---

## 源码目录树

```
src/main/java/com/duyell/communityfreshdelivery/
│
├── CommunityFreshDeliveryApplication.java   ← 启动入口
│
├── common/                                  ← 通用基础设施
│   ├── exception/
│   │   ├── BusinessException.java           ← 业务异常
│   │   └── GlobalExceptionHandler.java      ← 全局异常拦截
│   ├── result/
│   │   └── Result.java                      ← 统一响应体
│   ├── utils/
│   │   └── JwtUtil.java                     ← JWT 工具类
│   └── security/
│       ├── UserDetailsImpl.java             ← Spring Security 用户适配
│       └── UserDetailsServiceImpl.java      ← 用户加载服务
│
├── config/                                  ← Spring 配置
│   ├── MybatisPlusConfig.java               ← MyBatis-Plus 分页插件
│   └── Knife4jConfig.java                   ← Swagger 接口文档
│
├── entity/                                  ← 数据库实体
│   ├── User.java                            ← user 表
│   └── UserRole.java                        ← user_role 表
│
└── mapper/                                  ← MyBatis-Plus Mapper
    ├── UserMapper.java                      ← 继承 BaseMapper<User>
    └── UserRoleMapper.java                   ← 继承 BaseMapper<UserRole>
```

---

## 逐文件说明

### 1. 启动入口

| 项目 | 说明 |
|------|------|
| **文件** | `CommunityFreshDeliveryApplication.java` |
| **包** | `com.duyell.communityfreshdelivery` |
| **注解** | `@SpringBootApplication` |
| **功能** | Spring Boot 启动入口，`main()` 调用 `SpringApplication.run()`，自动开启组件扫描与自动配置 |
| **产出** | 启动后监听 8080 端口 |

---

### 2. 通用基础设施 — `common/`

#### 2.1 统一响应体

| 项目 | 说明 |
|------|------|
| **文件** | `Result.java` |
| **包** | `common.result` |
| **泛型** | `Result<T>` |
| **功能** | 所有 Controller 返回值统一包装为 `{code, message, data}` 结构 |
| **静态工厂** | `Result.ok()` / `Result.ok(data)` / `Result.ok(msg, data)` / `Result.fail(msg)` / `Result.fail(code, msg)` |
| **约定** | `code=200` 成功；400 参数错误；500 服务端错误；其余为业务错误码 |
| **依赖方** | 所有 Controller、`GlobalExceptionHandler` |

#### 2.2 业务异常

| 项目 | 说明 |
|------|------|
| **文件** | `BusinessException.java` |
| **包** | `common.exception` |
| **继承** | `RuntimeException` |
| **功能** | Service 层业务规则不满足时主动抛出，携带自定义错误码 |
| **构造器** | `BusinessException(message)` 默认 code=500；`BusinessException(code, message)` 自定义 |
| **错误码规划** | 10001-19999 → 商品/库存；20001-29999 → 订单/支付；30001-39999 → 用户/认证；40001-49999 → 配送/自提 |
| **捕获方** | `GlobalExceptionHandler` |

#### 2.3 全局异常拦截

| 项目 | 说明 |
|------|------|
| **文件** | `GlobalExceptionHandler.java` |
| **包** | `common.exception` |
| **注解** | `@RestControllerAdvice` |
| **功能** | 拦截所有 Controller 抛出的异常，统一包装为 `Result` 返回 |
| **拦截类型** | `BusinessException` → warn 日志 + 返回业务消息；`MethodArgumentNotValidException` → 拼接字段级校验错误；`Exception` → error 日志 + 兜底"服务器内部错误" |
| **扩展** | 后续可新增 `@ExceptionHandler` 方法拦截特定异常 |

#### 2.4 JWT 工具类

| 项目 | 说明 |
|------|------|
| **文件** | `JwtUtil.java` |
| **包** | `common.utils` |
| **注解** | `@Component` |
| **功能** | 签发 / 解析 / 校验 JWT 令牌 |
| **注入** | 构造注入 `app.jwt.secret`（签名密钥）+ `app.jwt.expiration`（有效期 ms） |
| **方法** | `generateToken(userId, roles)` → 签发 token；`getUserIdFromToken(token)` → 提取 userId；`isTokenValid(token)` → 校验合法性与过期 |
| **库** | jjwt 0.12.6（`Jwts.builder()` / `Jwts.parser().verifyWith()`） |
| **载荷** | `sub`=userId, `roles`=角色列表, `iat`=签发时间, `exp`=过期时间 |
| **依赖方** | `AuthService`（签发）、`JwtAuthenticationFilter`（校验） |

#### 2.5 Spring Security 用户适配

| 项目 | 说明 |
|------|------|
| **文件** | `UserDetailsImpl.java` |
| **包** | `common.security` |
| **实现** | `UserDetails` |
| **字段** | `userId: Long` + `password: String` + `roles: List<String>` |
| **功能** | 将项目自有用户模型适配为 Spring Security 可识别的 `UserDetails` |
| **`getAuthorities()`** | 角色列表 → `SimpleGrantedAuthority`（自动补 `ROLE_` 前缀） |
| **`getUsername()`** | 返回 userId 字符串 |
| **`getPassword()`** | 返回 BCrypt 加密密码，由 `DaoAuthenticationProvider` 自动比对 |
| **构造来源** | 登录时由 `UserDetailsServiceImpl` 构建；鉴权时由 `JwtAuthenticationFilter` 从 token 构建 |

#### 2.6 用户加载服务

| 项目 | 说明 |
|------|------|
| **文件** | `UserDetailsServiceImpl.java` |
| **包** | `common.security` |
| **实现** | `UserDetailsService` |
| **注入** | `UserMapper` + `UserRoleMapper`（`@RequiredArgsConstructor`） |
| **功能** | `loadUserByUsername(phone)` → 查 user 表得用户信息 + 查 user_role 表得角色列表 → 组装 UserDetailsImpl |
| **异常** | 手机号未注册 → `UsernameNotFoundException` |

---

### 3. Spring 配置 — `config/`

#### 3.1 MyBatis-Plus 配置

| 项目 | 说明 |
|------|------|
| **文件** | `MybatisPlusConfig.java` |
| **包** | `config` |
| **注解** | `@Configuration` |
| **功能** | 注册 `MybatisPlusInterceptor`，仅启用分页插件（MySQL 方言） |
| **扩展** | 后续可追加乐观锁、防全表更新/删除等拦截器 |
| **依赖** | `mybatis-plus-jsqlparser:3.5.9`（分页插件 3.5.9 起独立模块） |

#### 3.2 接口文档配置

| 项目 | 说明 |
|------|------|
| **文件** | `Knife4jConfig.java` |
| **包** | `config` |
| **注解** | `@Configuration` |
| **功能** | 配置 OpenAPI 3 文档元信息 + 接口分组，Knife4j 渲染 UI |
| **访问** | 启动后 `http://localhost:8080/doc.html` |
| **Bean** | `OpenAPI`（标题/版本/联系人）+ `GroupedOpenApi`（扫描全部 controller） |

---

### 4. 资源文件

#### 4.1 应用配置

| 项目 | 说明 |
|------|------|
| **文件** | `application.yml` |
| **路径** | `src/main/resources/` |
| **内容** | 端口（8080）、MySQL 数据源、Redis（Lettuce 连接池）、RabbitMQ（手动 ACK）、文件上传限制、MyBatis-Plus 配置（驼峰/逻辑删除/ID 自增）、Knife4j 启用、`app:` 自定义配置（OSS / JWT / 业务参数） |

#### 4.2 数据库初始化

| 项目 | 说明 |
|------|------|
| **文件** | `init.sql` |
| **路径** | `src/main/resources/` |
| **内容** | 建库 + 18 张业务表（含索引/注释）+ 初始数据（4 个测试用户 + 角色 + 6 条系统配置） |
| **规范** | 阿里巴巴规范（无外键/无级联）、逻辑删除、金额 DECIMAL(10,2)、订单编号=14位时间戳+8位随机码 |

---

---

### 5. 数据库实体 — `entity/`

#### 5.1 User

| 项目 | 说明 |
|------|------|
| **文件** | `User.java` |
| **映射** | `@TableName("user")` |
| **主键** | `id` 自增（`@TableId(type = IdType.AUTO)`） |
| **逻辑删除** | `deleted`（`@TableLogic`） |
| **字段** | id / phone / password / nickname / avatar / status / createTime / updateTime / deleted |

#### 5.2 UserRole

| 项目 | 说明 |
|------|------|
| **文件** | `UserRole.java` |
| **映射** | `@TableName("user_role")` |
| **字段** | id / userId / role / createTime / deleted |

---

### 6. Mapper 接口 — `mapper/`

#### 6.1 UserMapper

| 项目 | 说明 |
|------|------|
| **文件** | `UserMapper.java` |
| **继承** | `BaseMapper<User>` |
| **注解** | `@Mapper` |

#### 6.2 UserRoleMapper

| 项目 | 说明 |
|------|------|
| **文件** | `UserRoleMapper.java` |
| **继承** | `BaseMapper<UserRole>` |
| **注解** | `@Mapper` |

---

### 7. 测试

| 项目 | 说明 |
|------|------|
| **文件** | `EnvironmentCheckTest.java` |
| **路径** | `src/test/java/com/duyell/communityfreshdelivery/` |
| **功能** | 环境连通性全检 — MySQL / Redis / RabbitMQ / OSS，4 项全部通过 |

---

## 当前待扩展包（空目录）

| 包 | 计划内容 |
|------|------|
| `config/` | 后续追加 `SecurityConfig.java` |
| `common/security/` | 后续追加 `JwtAuthenticationFilter.java` |
| `controller/` | 各端 Controller |
| `service/` | Service 接口 + 实现 |
| `dto/` | 请求/响应 DTO |
