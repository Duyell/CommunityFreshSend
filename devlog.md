---

# 开发日志

## 项目信息

- 项目名称：社区生鲜配送系统
- 项目路径：`community-fresh-delivery`
- 技术栈：SpringBoot 4.0.6 + MyBatis-Plus 3.5.9 + MySQL 8.0 + Redis + RabbitMQ 4.3
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
| | `init.sql` 完整建表脚本 | 18 张业务表 + 初始数据（用户/角色/配置）+ 索引 |
| | Entity（User / UserRole） + Mapper | MyBatis-Plus 映射，逻辑删除 |
| | 订单编号规范 | 14 位时间戳 + 8 位随机码 |
| | `project-structure.md` 项目结构文档 | 逐文件功能说明 + 目录树 |

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

## 第二步 Spring Security + JWT 认证（进行中）

- [x] **2.1** JWT 工具类 `common/utils/JwtUtil.java`
- [x] **2.2** 创建 `common/security` 包
- [x] **2.3** UserDetails 实现类 `UserDetailsImpl.java` — userId + roles → GrantedAuthority
- [x] **2.4** UserDetailsService `UserDetailsServiceImpl.java` — 真实 Mapper 查询
- [x] `init.sql` 完整建表 — 18 张业务表 + 索引 + 初始数据
- [x] Entity + Mapper（User / UserRole）
- [x] `project-structure.md` 项目结构文档
- [ ] **2.5** JWT 认证过滤器 `JwtAuthenticationFilter.java`
- [ ] **2.6** SecurityConfig 安全配置
- [ ] **2.7** AuthController 登录接口
- [ ] **2.8** AuthController 注册接口

---

## 下次待做

- [ ] 2.5 JWT 认证过滤器
- [ ] 随后依次推进 2.6 ~ 2.8

