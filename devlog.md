---

# 开发日志

## 项目信息

- 项目名称：社区生鲜配送系统
- 项目路径：`community-fresh-delivery`
- 技术栈：SpringBoot 3.4.7 + MyBatis-Plus 3.5.9 + MySQL 8.0 + Redis + RabbitMQ 4.3
- Java 21 + Maven 3.9
- 远程仓库：已绑定

---

## 开发记录

| 日期 | 完成内容 | 备注 |
|------|---------|------|
| 2026-05-31 | 需求文档定稿（function.md） | 完成业务/技术/阶段规划 |
| | Git 初始化 + 远程仓库绑定 | |
| | SpringBoot 项目初始化 | IDEA 创建，pom.xml 依赖已修正（MyBatis-Plus/knife4j/jjwt/hutool/OSS） |
| | 环境搭建 | Erlang + RabbitMQ 4.3 安装，Windows 服务启动，ERLANG_HOME 配置 |
| | `application.yml` 基础配置 | MySQL/Redis/RabbitMQ/OSS/JWT/业务参数，自定义配置收归 `app:` 前缀 |
| | `init.sql` 数据库初始化脚本 | 建库 + 后续建表语句统一追加到此文件 |
| | `EnvironmentCheckTest` | 环境连通性全检：MySQL/Redis/RabbitMQ/OSS，4 项全部通过 |
| | `Result<T>` 统一返回结果 | `common/result/`，ok/fail 静态工厂 |
| | `BusinessException` 业务异常 | `common/exception/`，支持自定义 code |
| | `GlobalExceptionHandler` 全局异常拦截 | 业务异常 / 参数校验 / 兜底，统一包装为 Result |
| 2026-06-01 | MyBatis-Plus 分页插件（含 mybatis-plus-jsqlparser 依赖） | 3.5.9 拆分独立模块 |
| | Knife4j 接口文档配置 | OpenAPI 3，启动后访问 doc.html |
| | 全局代码注释规范 | 所有类补充 JavaDoc（功能/示例/扩展） |
| | JWT 工具类 JwtUtil | 构造注入，jjwt 0.12.6 |
| | UserDetailsImpl | userId + password + roles → GrantedAuthority |
| | UserDetailsServiceImpl | 注入 Mapper，真实查库 |
| | `init.sql` 完整建表脚本 | 19 张业务表 + 初始数据（用户/角色/配置）+ 索引 |
| | Entity（User / UserRole） + Mapper | MyBatis-Plus 映射，逻辑删除 |
| | 订单编号规范 | 14 位时间戳 + 8 位随机码 |
| | `project-structure.md` 项目结构文档 | 逐文件功能说明 + 目录树 |
| 2026-06-03 | `init.sql` 数据库优化 | review 表合并评分字段（freshness+match_score → score） |
| | BcryptPasswordGeneratorTest | 生成 "123456" 的 BCrypt 真实 hash，替换 init.sql 占位符 |
| | `init.sql` 本地执行 | 19 张表全部创建成功，测试数据写入无误 |
| | `function.md` 配送员模块完善 | 补全申请流程 + 模式切换 + 权限边界，与团长对称 |
| | 数据库设计回顾 | 确认 payment.idx_order_id / sys_config 设计合理性 |
| | JWT 认证过滤器 + SecurityConfig | JwtAuthenticationFilter + SecurityConfig（无状态 + 路由权限 + 401/403 JSON） |
| | 登录 & 注册接口全链路 | POST /api/auth/login + /register，AuthenticationManager + BCrypt + JWT |
| | Spring Boot 降级 4.0.6→3.4.7 | mybatis-plus-spring-boot3-starter 不兼容 Boot 4，降级后删手动装配 |
| 2026-06-04 | 包结构整理 | SecurityConfig 从 common/config 移至 config，消除双 config 包，所有配置类统一收归 config 包 |
| | 登录安全加固 | UserDetailsImpl.isEnabled() 对接 user.status 字段，禁用用户（status=0）无法登录，DaoAuthenticationProvider 前置检查拦截 |
| | JWT 登出功能 | Redis 黑名单方案：POST /api/auth/logout → token 写入 Redis（TTL 与 JWT 同步），JwtAuthenticationFilter 每次请求前查黑名单 |
| | Token 提取统一 | extractBearerToken 收敛到 JwtUtil（public static），JwtAuthenticationFilter 和 AuthController 共用同一入口 |
| | UserMapper.selectByPhone 修复 | 添加 @Param("phone") 注解，解决无 -parameters 编译时 MyBatis 参数名解析失败问题 |
| | 端到端回归测试 | 注册 → 登录 → 错误密码 → 重复注册 → 登出 → Redis 黑名单验证，全部通过 |
| | 分类树查询接口 | GET /api/category/tree，全表查内存组装，无限递归查询；测试数据 14 条（3 一级 + 11 子级） |
| | 商品 CRUD（商家端） | Product + ProductSku 主子表事务，创建/编辑/上下架/删除/详情/分页列表，@PreAuthorize 鉴权，SKU 先删后插 |
| | 方法级鉴权 | SecurityConfig 加 @EnableMethodSecurity，GlobalExceptionHandler 加 AccessDeniedException 处理 |
| | 端到端测试 | 分类树 + 商品 CRUD 全接口 + 鉴权（无token/普通用户/商家）全部通过 |
| 2026-06-06 | 商品列表（用户端） | GET /api/product/list（分类浏览 + 搜索合一，keyword 可选）+ GET /api/product/search（同调 listForUser），仅返回上架商品，支持 price_asc/price_desc 排序，默认时间降序 |
| | 代码规范修整 | @Transactional 补 rollbackFor；if 补大括号；消除行尾注释；消除重复代码（提取 toVOWithSkus/toVOPage）；消弭 queryForUser 空壳方法；合并 listForUser/searchForUser 为一个 Service 方法；sort 参数改为 optional 不设默认值 |
| | 商品列表端到端测试 | ProductBrowseTest — 8 个用例覆盖：全部分类/按分类/价格升降序/关键词搜索/空结果/分页/字段完整性，全部通过 |
| | 收货地址 CRUD | Address Entity/Mapper/Service/Controller 全套，5 个接口（列表/新增/编辑/删除/设默认），用户级隔离，最多10个，首个自动默认 |
| | 收货地址端到端测试 | AddressServiceTest — 7 个用例覆盖：新增列表/编辑/删除/设默认/首个自动默认/列表排序/全部删除，全部通过 |

---

## 当前进度：第一步 基础设施搭建（已完成 ✅）

- [x] **1.1** `application.yml` 基础配置
- [x] **1.2** `Result<T>` 统一返回结果
- [x] **1.3** `GlobalExceptionHandler` 全局异常处理
- [x] **1.4** `BusinessException` 业务异常类
- [x] **1.5** MyBatis-Plus 配置类 — 分页插件（3.5.9 需单独引入 mybatis-plus-jsqlparser）
- [x] **1.6** Knife4j 配置类 — Swagger 文档分组（OpenAPI 3 + GroupedOpenApi）
- [x] 全局代码注释规范 — 所有类补充完整 JavaDoc（功能说明 + 使用示例 + 扩展指南）
- [x] **2.1** JWT 工具类 — JwtUtil（构造注入，jjwt 0.12.6，密钥/过期时间从 yml 读取）

## 第二步 Spring Security + JWT 认证（已完成 ✅）

- [x] **2.1** JWT 工具类 `common/utils/JwtUtil.java`
- [x] **2.2** 创建 `common/security` 包
- [x] **2.3** UserDetails 实现类 `UserDetailsImpl.java`
- [x] **2.4** UserDetailsService `UserDetailsServiceImpl.java`
- [x] `init.sql` 完整建表 — 19 张业务表 + 索引 + 初始数据
- [x] Entity + Mapper（User / UserRole）
- [x] 数据库本地初始化 → 19 张表全部建好
- [x] **2.5** JWT 认证过滤器 `JwtAuthenticationFilter.java`
- [x] **2.6** SecurityConfig 安全配置
- [x] **2.7** AuthController 登录接口
- [x] **2.8** AuthController 注册接口
- [x] **2.9** 禁用用户拦截 — `isEnabled()` 对接 `user.status`
- [x] **2.10** JWT 登出 — Redis 黑名单 + 过滤器拦截
- [x] 端到端回归测试 → 注册/登录/错误密码/重复注册/登出/黑名单验证，全部通过
- [x] Spring Boot 版本稳定 → 降级 3.4.7，去除手动装配补丁

---

## 第三步 商品模块（已完成 ✅）

- [x] **3.1** 分类接口 — 分类树查询（GET /api/category/tree）
- [x] **3.2** 商品 CRUD — 商家端商品管理（创建/编辑/上下架/删除/详情/分页 + 鉴权）
- [x] **3.3** 商品列表 — 用户端分类浏览 + 搜索（GET /api/product/list + /search）

---

## 第四步 收货地址模块（已完成 ✅）

- [x] **4.1** Address Entity + Mapper + DTO + Service + Controller — 5 个接口（列表/新增/编辑/删除/设默认）
- [x] 用户级隔离（SecurityContext 取当前用户）、最多 10 个地址、首个自动默认、默认地址排最前

---

## 下次待做

- [ ] **第五步** Redis 购物车 — CartService（Redis Hash 存储，add/remove/updateQty/list/clear）

