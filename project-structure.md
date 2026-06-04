---
# 项目结构文档

> 社区生鲜配送系统 — 完整项目文件索引与功能说明  
> 更新日期：2026-06-04

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
Maven 项目描述文件。定义坐标 `com.duyell:community-fresh-delivery:0.0.1-SNAPSHOT`，继承 `spring-boot-starter-parent:3.4.7`（Java 21）。

**Why Boot 3.4.7：** `mybatis-plus-spring-boot3-starter:3.5.9` 仅兼容 Spring Boot 3.x，Boot 4.x 下 SqlSessionFactory 自动装配失效。生态成熟后升级。

**依赖清单：**
| 依赖 | 用途 |
|------|------|
| `spring-boot-starter-web` | REST 接口 + 内嵌 Tomcat |
| `spring-boot-starter-security` | 认证鉴权框架 |
| `spring-boot-starter-data-redis` | Redis 缓存 / 分布式锁 |
| `spring-boot-starter-amqp` | RabbitMQ 消息队列 |
| `spring-boot-starter-validation` | Bean Validation 参数校验 |
| `spring-boot-starter-test` | 测试框架（含 JUnit 5 / MockMvc） |
| `mybatis-plus-spring-boot3-starter` 3.5.9 | ORM 框架 |
| `mybatis-plus-jsqlparser` 3.5.9 | 分页 SQL 解析（3.5.9 起独立模块） |
| `mysql-connector-j` | MySQL 驱动（runtime） |
| `knife4j-openapi3-jakarta-spring-boot-starter` 4.5.0 | Swagger 接口文档 |
| `jjwt-api/impl/jackson` 0.12.6 | JWT 令牌签发 / 解析 |
| `aliyun-sdk-oss` 3.18.1 | 阿里云 OSS 文件上传 |
| `hutool-all` 5.8.30 | 通用工具库 |
| `lombok` | 简化 getter/setter/构造器（optional） |

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
│   │   └── GlobalExceptionHandler.java      ← 全局异常拦截（含认证失败 401）
│   ├── result/
│   │   └── Result.java                      ← 统一响应体
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java     ← JWT 认证过滤器
│   │   ├── UserDetailsImpl.java             ← Spring Security 用户适配
│   │   └── UserDetailsServiceImpl.java      ← 用户加载服务
│   └── utils/
│       └── JwtUtil.java                     ← JWT 工具类
│
├── config/                                  ← Spring 配置
│   ├── SecurityConfig.java                  ← Spring Security 配置（JWT 过滤链 + 权限）
│   ├── MybatisPlusConfig.java               ← MyBatis-Plus 分页插件
│   └── Knife4jConfig.java                   ← Swagger 接口文档
│
├── controller/                              ← 接口层
│   ├── AuthController.java                  ← 认证接口（登录/注册/登出）
│   ├── CategoryController.java              ← 分类接口（分类树查询）
│   └── ProductController.java               ← 商品接口（CRUD + 分页）
│
├── dto/                                     ← 数据传输对象
│   ├── LoginDTO.java                        ← 登录请求
│   ├── LoginVO.java                         ← 登录响应
│   ├── RegisterDTO.java                     ← 注册请求
│   ├── RegisterVO.java                      ← 注册响应
│   ├── CategoryVO.java                      ← 分类树节点
│   ├── ProductSaveDTO.java                  ← 商品创建/编辑请求（含 SKU） 
│   └── ProductVO.java                       ← 商品响应（含 SKU）
│
├── entity/                                  ← 数据库实体
│   ├── User.java                            ← user 表
│   ├── UserRole.java                        ← user_role 表
│   ├── Category.java                        ← category 表
│   ├── Product.java                         ← product 表
│   └── ProductSku.java                      ← product_sku 表
│
├── mapper/                                  ← MyBatis-Plus Mapper
│   ├── UserMapper.java                      ← 继承 BaseMapper<User> + selectByPhone()
│   ├── UserRoleMapper.java                  ← 继承 BaseMapper<UserRole>
│   ├── CategoryMapper.java                  ← 继承 BaseMapper<Category>
│   ├── ProductMapper.java                   ← 继承 BaseMapper<Product>
│   └── ProductSkuMapper.java                ← 继承 BaseMapper<ProductSku> + deleteByProductId()
│
└── service/                                 ← 业务层
    ├── AuthService.java                     ← 认证服务接口
    ├── CategoryService.java                 ← 分类服务接口
    ├── ProductService.java                  ← 商品服务接口
    └── impl/
        ├── AuthServiceImpl.java             ← 认证服务实现
        ├── CategoryServiceImpl.java         ← 分类服务实现
        └── ProductServiceImpl.java          ← 商品服务实现
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
| **拦截类型** | `BusinessException` → 业务消息；`AuthenticationException` → "手机号或密码错误"；`AccessDeniedException` → "权限不足"；`MethodArgumentNotValidException` → 字段校验错误；`Exception` → 兜底"服务器内部错误" |
| **扩展** | 后续可新增 `@ExceptionHandler` 方法拦截特定异常 |

#### 2.4 JWT 工具类

| 项目 | 说明 |
|------|------|
| **文件** | `JwtUtil.java` |
| **包** | `common.utils` |
| **注解** | `@Component` |
| **功能** | 签发 / 解析 / 校验 JWT 令牌 |
| **注入** | 构造注入 `app.jwt.secret`（签名密钥）+ `app.jwt.expiration`（有效期 ms） |
| **方法** | `generateToken(userId, roles)` → 签发 token；`getUserIdFromToken(token)` → 提取 userId；`getRolesFromToken(token)` → 提取角色列表；`isTokenValid(token)` → 校验合法性与过期；`getRemainingExpiration(token)` → 获取剩余有效时间（毫秒）；`extractBearerToken(request)` → 从 Authorization 头截取 Bearer token（static） |
| **库** | jjwt 0.12.6（`Jwts.builder()` / `Jwts.parser().verifyWith()`） |
| **载荷** | `sub`=userId, `roles`=角色列表, `iat`=签发时间, `exp`=过期时间 |
| **依赖方** | `AuthServiceImpl`（签发/登出）、`JwtAuthenticationFilter`（校验/提取）、`AuthController`（登出时提取 token） |

#### 2.5 Spring Security 用户适配

| 项目 | 说明 |
|------|------|
| **文件** | `UserDetailsImpl.java` |
| **包** | `common.security` |
| **实现** | `UserDetails` |
| **字段** | `userId: Long` + `password: String` + `roles: List<String>` + `enabled: boolean` |
| **功能** | 将项目自有用户模型适配为 Spring Security 可识别的 `UserDetails` |
| **`getAuthorities()`** | 角色列表 → `SimpleGrantedAuthority`（自动补 `ROLE_` 前缀） |
| **`getUsername()`** | 返回 userId 字符串 |
| **`getPassword()`** | 返回 BCrypt 加密密码，由 `DaoAuthenticationProvider` 自动比对 |
| **`isEnabled()`** | 返回 user.status 对应的启用状态，status==1 → true，DaoAuthenticationProvider 前置检查拦截禁用用户 |
| **构造来源** | 登录时由 `UserDetailsServiceImpl` 构建（含 enabled）；鉴权时由 `JwtAuthenticationFilter` 从 token 构建（enabled=true） |

#### 2.6 用户加载服务

| 项目 | 说明 |
|------|------|
| **文件** | `UserDetailsServiceImpl.java` |
| **包** | `common.security` |
| **实现** | `UserDetailsService` |
| **注入** | `UserMapper` + `UserRoleMapper`（`@RequiredArgsConstructor`） |
| **功能** | `loadUserByUsername(phone)` → 查 user 表得用户信息 + status → 查 user_role 表得角色列表 → 组装 UserDetailsImpl（含 enabled=status==1） |
| **异常** | 手机号未注册 → `UsernameNotFoundException`；status=0 → `DisabledException`（由 DaoAuthenticationProvider 前置检查抛出） |

#### 2.7 JWT 认证过滤器

| 项目 | 说明 |
|------|------|
| **文件** | `JwtAuthenticationFilter.java` |
| **包** | `common.security` |
| **继承** | `OncePerRequestFilter`（保证每个请求只执行一次） |
| **注入** | `JwtUtil` + `StringRedisTemplate`（`@RequiredArgsConstructor`） |
| **功能** | 从 `Authorization: Bearer <token>` 头截取 JWT → 校验合法性 → 检查 Redis 黑名单（已登出则拦截）→ 解析 userId + roles → 构建 `UserDetailsImpl` → 写入 `SecurityContextHolder` |
| **关键设计** | 无状态（不查数据库，完全信任 JWT 载荷）；Redis 黑名单弥补 JWT 无法主动失效的缺陷；静默失败（token 不合法/已登出时不抛异常，由 SecurityConfig 返回 401） |

---

### 3. 接口层 — `controller/`

#### 3.1 AuthController

| 项目 | 说明 |
|------|------|
| **文件** | `AuthController.java` |
| **包** | `controller` |
| **注解** | `@RestController` + `@RequestMapping("/api/auth")` |
| **注入** | `AuthService` |
| **接口** | `POST /api/auth/login` — 登录；`POST /api/auth/register` — 注册；`POST /api/auth/logout` — 登出（token 加入 Redis 黑名单） |
| **参数校验** | `@Valid` 自动触发 Bean Validation，校验失败由 `GlobalExceptionHandler` 统一返回 400 |

#### 3.2 CategoryController

| 项目 | 说明 |
|------|------|
| **文件** | `CategoryController.java` |
| **包** | `controller` |
| **注解** | `@RestController` + `@RequestMapping("/api/category")` |
| **注入** | `CategoryService` |
| **接口** | `GET /api/category/tree` — 获取分类树（公开接口，无需认证） |

#### 3.3 ProductController

| 项目 | 说明 |
|------|------|
| **文件** | `ProductController.java` |
| **包** | `controller` |
| **注解** | `@RestController` + `@RequestMapping("/api/product")` |
| **注入** | `ProductService` |
| **接口** | `POST /api/product` — 创建（@PreAuthorize ROLE_MERCHANT）；`PUT /api/product/{id}` — 编辑；`PUT /api/product/{id}/status` — 上下架；`DELETE /api/product/{id}` — 删除；`GET /api/product/{id}` — 详情（公开）；`GET /api/product/page` — 分页列表（@PreAuthorize） |

---

### 4. 业务层 — `service/`

#### 4.1 AuthService 接口

| 项目 | 说明 |
|------|------|
| **文件** | `AuthService.java` |
| **包** | `service` |
| **方法** | `login/register/logout` |

#### 4.2 AuthServiceImpl 实现

| 项目 | 说明 |
|------|------|
| **文件** | `AuthServiceImpl.java` |
| **包** | `service.impl` |
| **注入** | `AuthenticationManager` + `JwtUtil` + `UserMapper` + `UserRoleMapper` + `PasswordEncoder` + `StringRedisTemplate` |
| **登录链路** | `AuthenticationManager.authenticate()` → `UserDetailsServiceImpl.loadUserByUsername()` → `DaoAuthenticationProvider` 比对 BCrypt（含 enabled 状态校验）→ 签发 JWT → 返回 `LoginVO` |
| **注册链路** | `selectByPhone()` 校验唯一性 → BCrypt 加密 → INSERT user + INSERT user_role（`@Transactional`）→ 签发 JWT（注册即登录）→ 返回 `RegisterVO` |
| **登出链路** | 校验 token 有效性 → 计算剩余过期时间 → 写入 Redis 黑名单（TTL 与 token 同步） |
| **异常** | 手机号已注册 → `BusinessException(30001)`；认证失败 → `BadCredentialsException`（由 `GlobalExceptionHandler` 转 401） |

#### 4.3 CategoryService 接口

| 项目 | 说明 |
|------|------|
| **文件** | `CategoryService.java` |
| **包** | `service` |
| **方法** | `getCategoryTree()` → `List<CategoryVO>` |

#### 4.4 CategoryServiceImpl 实现

| 项目 | 说明 |
|------|------|
| **文件** | `CategoryServiceImpl.java` |
| **包** | `service.impl` |
| **注入** | `CategoryMapper` |
| **功能** | 一次性查全表（按 sort 排序）→ 按 parentId 分组 → 递归组装 CategoryVO 树 |
| **设计要点** | 不做递归 SQL（N+1），分类总量有限全量加载无性能问题 |

#### 4.5 ProductService 接口

| 项目 | 说明 |
|------|------|
| **文件** | `ProductService.java` |
| **包** | `service` |
| **方法** | `create/update/updateStatus/delete/getById/page` |

#### 4.6 ProductServiceImpl 实现

| 项目 | 说明 |
|------|------|
| **文件** | `ProductServiceImpl.java` |
| **包** | `service.impl` |
| **注入** | `ProductMapper` + `ProductSkuMapper` |
| **关键设计** | 主子表事务（create/update）、SKU 先删后插、软删除（@TableLogic）、分页列表取最低售价 |
| **异常** | 商品不存在 → `BusinessException(10001)` |

---

### 5. 数据传输对象 — `dto/`

| 文件 | 用途 | 校验注解 |
|------|------|---------|
| `LoginDTO.java` | 登录请求 | `@NotBlank` phone / password |
| `LoginVO.java` | 登录响应 | userId / phone / nickname / avatar / token / roles |
| `RegisterDTO.java` | 注册请求 | `@NotBlank` + `@Pattern`（手机号）/ `@Size(6-20)`（密码）/ `@Size(max=50)`（昵称） |
| `RegisterVO.java` | 注册响应 | userId / phone / nickname / token / roles（注册即登录） |
| `CategoryVO.java` | 分类树节点 | id / name / sort / children（递归结构，前端直接渲染级联菜单） |
| `ProductSaveDTO.java` | 商品创建/编辑请求 | categoryId / name / description / images / isWeighted / skus（内嵌 SkuItem 列表） |
| `ProductVO.java` | 商品响应 | id / name / status / minPrice / skus（内嵌 SkuVO 列表），列表页 skus 不填充 |

---

### 6. Spring 配置 — `config/`

#### 6.1 Spring Security 配置

| 项目 | 说明 |
|------|------|
| **文件** | `SecurityConfig.java` |
| **包** | `config` |
| **注解** | `@Configuration` + `@EnableWebSecurity` + `@EnableMethodSecurity` |
| **注入** | `JwtAuthenticationFilter` |
| **Bean** | `SecurityFilterChain`（关闭 CSRF / 无状态 Session / URL 层放行公开路径 / 注册 JWT 过滤器 / 401&403 JSON 响应）；`AuthenticationManager`；`PasswordEncoder`（BCrypt） |
| **方法安全** | `@EnableMethodSecurity` 启用 `@PreAuthorize`，Controller 层按角色控制（如 `hasRole('MERCHANT')`） |
| **公开路径** | `/api/auth/login`、`/api/auth/register`、`/api/auth/logout`、Knife4j 文档路径 |
| **其余路径** | 一律要求认证 |

#### 6.2 MyBatis-Plus 配置

| 项目 | 说明 |
|------|------|
| **文件** | `MybatisPlusConfig.java` |
| **包** | `config` |
| **注解** | `@Configuration` |
| **功能** | 注册 `MybatisPlusInterceptor`，仅启用分页插件（MySQL 方言）。SqlSessionFactory 和 Mapper 扫描由 starter 自动配置 |
| **扩展** | 后续可追加乐观锁、防全表更新/删除等拦截器 |

#### 6.3 接口文档配置

| 项目 | 说明 |
|------|------|
| **文件** | `Knife4jConfig.java` |
| **包** | `config` |
| **注解** | `@Configuration` |
| **功能** | 配置 OpenAPI 3 文档元信息 + 接口分组，Knife4j 渲染 UI |
| **访问** | 启动后 `http://localhost:8080/doc.html` |
| **Bean** | `OpenAPI`（标题/版本/联系人）+ `GroupedOpenApi`（扫描全部 controller） |

---

### 7. 资源文件

#### 7.1 应用配置

| 项目 | 说明 |
|------|------|
| **文件** | `application.yml` |
| **路径** | `src/main/resources/` |
| **内容** | 端口（8080）、MySQL 数据源、Redis（Lettuce 连接池）、RabbitMQ（手动 ACK）、文件上传限制、MyBatis-Plus 配置（驼峰/逻辑删除/ID 自增）、Knife4j 启用、`app:` 自定义配置（OSS / JWT / 业务参数） |

#### 7.2 数据库初始化

| 项目 | 说明 |
|------|------|
| **文件** | `init.sql` |
| **路径** | `src/main/resources/` |
| **内容** | 建库 + 19 张业务表（含索引/注释）+ 初始数据（4 个测试用户 + 角色 + 6 条系统配置） |
| **规范** | 阿里巴巴规范（无外键/无级联）、逻辑删除、金额 DECIMAL(10,2)、订单编号=14位时间戳+8位随机码 |

---

### 8. 数据库实体 — `entity/`

#### 8.1 User

| 项目 | 说明 |
|------|------|
| **文件** | `User.java` |
| **映射** | `@TableName("user")` |
| **主键** | `id` 自增（`@TableId(type = IdType.AUTO)`） |
| **逻辑删除** | `deleted`（`@TableLogic`） |
| **字段** | id / phone / password / nickname / avatar / status / createTime / updateTime / deleted |

#### 8.2 UserRole

| 项目 | 说明 |
|------|------|
| **文件** | `UserRole.java` |
| **映射** | `@TableName("user_role")` |
| **字段** | id / userId / role / createTime / deleted |

#### 8.3 Category

| 项目 | 说明 |
|------|------|
| **文件** | `Category.java` |
| **映射** | `@TableName("category")` |
| **主键** | `id` 自增（`@TableId(type = IdType.AUTO)`） |
| **逻辑删除** | `deleted`（`@TableLogic`） |
| **字段** | id / parentId（0=一级分类） / name / sort / status（1=启用 0=停用） / createTime / updateTime / deleted |

#### 8.4 Product

| 项目 | 说明 |
|------|------|
| **文件** | `Product.java` |
| **映射** | `@TableName("product")` |
| **字段** | id / categoryId / name / description / images（JSON 数组字符串）/ status（1=上架 2=下架 3=售罄）/ isWeighted（0=固定规格 1=称重）/ createTime / updateTime / deleted |

#### 8.5 ProductSku

| 项目 | 说明 |
|------|------|
| **文件** | `ProductSku.java` |
| **映射** | `@TableName("product_sku")` |
| **字段** | id / productId / specName / price（DECIMAL）/ stock / stockThreshold / status / createTime / updateTime / deleted |

---

### 9. Mapper 接口 — `mapper/`

#### 9.1 UserMapper

| 项目 | 说明 |
|------|------|
| **文件** | `UserMapper.java` |
| **继承** | `BaseMapper<User>` |
| **注解** | `@Mapper` |
| **自定义方法** | `selectByPhone(@Param("phone") String phone)` — 根据手机号查用户（`@Select` 注解 SQL，`@Param` 确保参数名在编译后保留） |

#### 9.2 UserRoleMapper

| 项目 | 说明 |
|------|------|
| **文件** | `UserRoleMapper.java` |
| **继承** | `BaseMapper<UserRole>` |
| **注解** | `@Mapper` |

#### 9.3 CategoryMapper

| 项目 | 说明 |
|------|------|
| **文件** | `CategoryMapper.java` |
| **继承** | `BaseMapper<Category>` |
| **注解** | `@Mapper` |

#### 9.4 ProductMapper

| 项目 | 说明 |
|------|------|
| **文件** | `ProductMapper.java` |
| **继承** | `BaseMapper<Product>` |
| **注解** | `@Mapper` |

#### 9.5 ProductSkuMapper

| 项目 | 说明 |
|------|------|
| **文件** | `ProductSkuMapper.java` |
| **继承** | `BaseMapper<ProductSku>` |
| **注解** | `@Mapper` |
| **自定义方法** | `deleteByProductId(productId)` — 物理删除商品下所有 SKU（@Delete 注解，用于编辑时"先删后插"） |

---

### 10. 测试

| 文件 | 功能 |
|------|------|
| `EnvironmentCheckTest.java` | 环境连通性全检 — MySQL / Redis / RabbitMQ / OSS |
| `BcryptPasswordGeneratorTest.java` | 生成 "123456" 的 BCrypt hash，输出到控制台供 init.sql 使用 |
| `AuthRegisterTest.java` | 注册流程集成测试（`@SpringBootTest`，直调 Service） |
