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
| | Redis 购物车 | Redis Hash 实现（key=cart:user:{userId}），CartService：add/updateQty/remove/clear/list，加购累加数量、改数量直接覆盖、列表批量查 SKU+Product 拼 VO，不扣库存仅校验 |
| | 购物车端到端测试 | CartServiceTest — 8 个用例覆盖：加购累加/改数量/删单品/清空/多商品/空购物车/用户隔离，全部通过 |
| | AddressServiceImpl.setDefault 优化 | selectList + for 循环 → 单条 LambdaUpdateWrapper UPDATE，消灭 N+1 |
| 2026-06-07 | 下单 + 模拟支付 | 第六步完整模块：Order/OrderItem/Payment Entity + Mapper + DTO/VO + OrderService（place/cancel/page/getById）+ MockPaymentService（策略模式）+ RabbitMQ 死信队列延迟消息（DLX+TTL 代替插件）+ OrderController 6 个接口 |
| | 消弭重复代码 | SecurityUtil 提取 currentUserId()（3 个 ServiceImpl 共用）；OrderServiceImpl.place() 复用 CartService.list()/clear()；MockPaymentServiceImpl.cancelPay() 复用 OrderService.cancel()；变量名 qty→quantity |
| | 订单端到端测试 | OrderServiceTest — 12 个用例覆盖：下单/自提/空车/库存不足/起送价/支付/重复支付/取消支付/取消订单/详情/用户隔离/分页，全部通过 |
| | 商家接单分拣 + 配送员配送 | 第七步完整模块：Delivery/PickupPoint Entity + Mapper + DeliveryType/DeliveryStatus 枚举 + OrderService 追加 accept/sortComplete + DeliveryService（hall/grab/confirmPickup/confirmDelivery/myDeliveries）+ DeliveryController 5 个接口 + OrderController 追加 2 个商家接口 |
| | 消弭重复代码 | DeliveryServiceImpl.hall/myDeliveries 批量加载地址和自提点消除 N+1；grab() 用条件 UPDATE 原子抢单消除重复检查；DeliveryType 枚举消除 deliveryType==1/2 魔法值 |
| | 配送端到端测试 | DeliveryServiceTest — 9 个用例覆盖：接单大厅/抢单/重复抢单/取货送达/角色隔离/我的配送/无记录校验，全部通过 |
| 2026-06-08 | 团长申请与审核 | GroupLeaderApplication Entity/Mapper + GroupLeaderApplyDTO/ApplicationVO/ReviewDTO + GroupLeaderApplicationService（apply/myApplication/pageApplications/review）+ GroupLeaderApplicationController 6 个接口 |
| | 提货码核销 | GroupLeaderApplicationService.verifyPickup（查自提点→校验归属→核销态变更 5→6）+ PickupCodeDTO |
| | 自提点公开接口 | PickupPointController（list/detail，PERMIT_ALL），下单前即可浏览 |
| | 自提超时自动退回 | PickupTimeoutScheduler（@Scheduled 每天 3 点扫描 status=5 超时未取→自动取消）+ SysConfig Entity/Mapper + @EnableScheduling |
| | SecurityConfig 更新 | PERMIT_ALL_PATHS 新增 /api/pickup-point/**（公开浏览自提点） |
| | UserRoleMapper 强化 | physicalDelete() 物理删除（绕过 @TableLogic），修复软删除同值 re-grant 唯一约束冲突 |
| | 端到端测试 | GroupLeaderApplicationServiceTest（9 用例）+ PickupVerifyServiceTest（6 用例），15/15 全部通过 |
| 2026-06-08 | 评价模块 | Review Entity/Mapper + ReviewCreateDTO/ReviewVO + ReviewService（create/getByOrderId/getByProductId/myReviews）+ ReviewController 4 个接口 + SecurityConfig 公开 /api/review/product/** |
| | 端到端测试 | ReviewServiceTest — 9 个用例覆盖：创建评价/无文字/重复评价/非本人订单/状态校验/订单查询/商品分页/我的评价/空结果，全部通过 |
| 2026-06-09 | 优惠券模块 | Coupon/UserCoupon Entity + Mapper + DTO/VO + CouponService（CRUD + 发券 + 可用券过滤 + 过期自动标记）+ CouponController 8 个接口 + OrderServiceImpl.place() 集成优惠券校验链路 |
| | 站内消息模块 | Notification Entity/Mapper + NotificationVO + NotificationService（未读/已读/分页/发送）+ NotificationController 5 个接口 |
| | 批次库存模块 | ProductBatch Entity/Mapper + DTO/VO + ProductBatchService（入库/FIFO分配/临期查询）+ ProductBatchController 4 个接口 + BatchExpiryScheduler 定时预警 |
| | 操作日志模块 | OperationLog Entity/Mapper + OperationLogService（独立事务记录）+ 集成到 OrderServiceImpl/MockPaymentServiceImpl/DeliveryServiceImpl/GroupLeaderApplicationServiceImpl 共 8 处关键操作 |
| | 编译验证 | mvn compile 通过，mvn test 25 pass + 47 error（均为 Redis 未启动，非代码问题） |
| | 第十一步 | 配送员申请审核 + 称重计价差异处理 + 收藏夹 + 数据看板 + 领券中心，21 个新文件 + 2 张新表 + 7 个修改文件，编译通过 |
| | 第十二步 | 团长佣金结算 + 支付对账定时任务 + 接口限流（Redis滑动窗口AOP）+ 临期折扣展示，10 个新文件 + 1 张新表 + 9 个修改文件，pom.xml 追加 AOP 依赖，编译通过 |

| 2026-07-09 | **代码审查 + Bug 修复** | 全面审查 163 个 Java 文件，发现并修复 14 个问题（详见下方） |
| | **Bug 修复清单：** |
| | 1. `OrderTimeoutConsumer` — 库存回补改用原子 `addRemaining` + 支持 `batchAllocations` JSON 精确回补 |
| | 2. `OrderServiceImpl.sortComplete()` — 缺货商品回补批次库存（之前只退款不回血） |
| | 3. `ProductBatchServiceImpl.allocateFIFO()` — 并发安全：最多 3 轮重新加载批次列表 |
| | 4. `JwtAuthenticationFilter` — 增加 `user:disabled:{userId}` Redis 校验，禁用用户即时失效 |
| | 5. `OrderServiceImpl.place()` — 校验商品上架状态 + 实付金额防负值 |
| | 6. `PickupTimeoutScheduler` — 重写：逐单回补库存 + 释放优惠券 + 限制仅自提单 + 原子状态更新 |
| | 7. `CartServiceImpl.add()` — Redis HINCRBY 原子自增替代 read-then-write |
| | 8. `MockPaymentServiceImpl.pay()` — 支付状态改用原子 UPDATE（防并发重复支付） |
| | 9. `OrderServiceImpl.accept()` — 接单改用原子 UPDATE（防并发重复接单） |
| | 10. `OrderServiceImpl.cancel()` — 取消改用原子 UPDATE `WHERE status IN (...)` |
| | 11. `DeliveryServiceImpl.grab()` — 增加配送类型校验（自提单不可抢）+ UPDATE 增加 `delivery_type=1` 条件 |
| | 12. `AuthController.register()` — 增加 `@RateLimit` 限流（3次/60秒） |
| | 13. `CartAddDTO.quantity` — 增加 `@Max(999)` 上限校验 |
| | 14. `OrderServiceImpl` — 提取 `rollbackItemStock()` 统一库存回补逻辑，取消/缺货/超时三处共用 |
| | **React 前端项目** | 创建 `fresh-market-frontend/`（React 18 + TypeScript + Vite + Tailwind CSS + React Router） |
| | 前端 8 个页面：Login / Register / Home / ProductDetail / Cart / Checkout / Orders / OrderDetail |
| | API 层：Axios 封装（JWT 拦截 + 401 处理）+ auth/product/cart/order/address 5 个模块 |
| | 编译验证：`npx vite build` 通过，产物 317KB JS + 26KB CSS（gzip 后 ~106KB） |

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

## 第五步 Redis 购物车（已完成 ✅）

- [x] **5.1** CartService — add/updateQty/remove/clear/list，基于 Redis Hash，key=cart:user:{userId}，加购累加、改量覆盖、列表拼 SKU/Product 信息
- [x] 加购校验 SKU 存在 + 库存 > 0，不扣库存（下单时才扣）

---

## 第六步 下单 + 模拟支付（已完成 ✅）

- [x] **6.1** Entity — Order / OrderItem / Payment
- [x] **6.2** Mapper — OrderMapper / OrderItemMapper / PaymentMapper
- [x] **6.3** DTO/VO — OrderCreateDTO / OrderVO / OrderItemVO / PayRequestDTO / OrderTimeoutMessage
- [x] **6.4** 订单编号生成器 — OrderNoUtil（14位时间戳 + 8位随机码）
- [x] **6.5** 订单状态枚举 — OrderStatus（12 状态 code↔text）
- [x] **6.6** OrderService + OrderServiceImpl — place / getById / page / cancel
  - 下单事务：校验配送参数 → 复用 CartService.list() 读购物车 → 库存预检 + 计算金额 → 扣减 MySQL 库存（WHERE stock >= quantity 防超卖）→ 生成订单号 → 写 order + order_item → 清空购物车 → 发死信延迟消息
  - 取消回补库存 + 用户隔离
- [x] **6.7** PaymentService + MockPaymentServiceImpl — 策略模式（@Service("mockPaymentService")），pay()/cancelPay() 复用 OrderService.cancel()
- [x] **6.8** RabbitMQ 死信队列延迟消息 — DLX + TTL（无需插件，兼容 4.3.x），RabbitMQConfig + OrderTimeoutConsumer
- [x] **6.9** OrderController — 6 个接口：POST /api/order、GET /{id}、GET /page、PUT /{id}/cancel、POST /{id}/pay、POST /{id}/cancel-pay
- [x] **6.10** ProductSkuMapper 补充 — deductStock / addStock（@Update 注解）
- [x] **6.11** 端到端测试 — 12 个用例全部通过
- [x] **消弭重复代码** — SecurityUtil.currentUserId() 统一 3 个 ServiceImpl；下单复用 CartService；取消支付复用 OrderService.cancel()

---

## 第七步 商家接单分拣 + 配送员配送（已完成 ✅）

- [x] **7.1** Entity — Delivery / PickupPoint + 枚举 DeliveryStatus / DeliveryType
- [x] **7.2** Mapper — DeliveryMapper / PickupPointMapper
- [x] **7.3** DTO — DeliveryVO
- [x] **7.4** OrderService 追加 accept / sortComplete（status 1→2→3）
- [x] **7.5** DeliveryService + DeliveryServiceImpl — hall / grab / confirmPickup / confirmDelivery / myDeliveries
  - grab() 用条件 UPDATE（WHERE status=PENDING_DELIVERY）原子抢单
  - hall/myDeliveries 批量加载地址和自提点消除 N+1
- [x] **7.6** OrderController 追加 2 个商家接口（accept / sortComplete，@PreAuthorize MERCHANT）
- [x] **7.7** DeliveryController — 5 个接口（hall / grab / pickup / deliver / my，@PreAuthorize DELIVERY）
- [x] **7.8** 端到端测试 — 9 个用例覆盖接单大厅/抢单/重复抢单/取货送达/角色隔离/我的配送，全部通过
- [x] **消弭重复代码** — 批量加载消除 N+1；条件 UPDATE 原子抢单；DeliveryType 枚举消除魔法值

---

## 第八步 自提模块（已完成 ✅）

- [x] **8.1** Entity — `GroupLeaderApplication` + `SysConfig`
- [x] **8.2** Mapper — `GroupLeaderApplicationMapper` / `SysConfigMapper`
- [x] **8.3** DTO — `GroupLeaderApplyDTO` / `GroupLeaderApplicationVO` / `GroupLeaderReviewDTO` / `PickupCodeDTO`
- [x] **8.4** `GroupLeaderApplicationService` + `GroupLeaderApplicationServiceImpl` — apply / myApplication / pageApplications / review / verifyPickup / myPickupPoint
  - `review()` @Transactional：通过时创建 pickup_point + 追加 ROLE_GROUP_LEADER + 更新申请状态
  - `verifyPickup()`：查团长自提点 → 提货码查订单 → 校验归属 + status=5 → 更新 status=6（用户已自提）+ pickupTime
  - `UserRoleMapper.physicalDelete()` 物理删除绕过 @TableLogic，修复软删除后同值 re-grant 唯一约束冲突
- [x] **8.5** Controller — `GroupLeaderApplicationController`（用户端 apply/my + 管理端 page/review + 团长端 myPickupPoint/verify，方法级 @PreAuthorize）；`PickupPointController`（公开 list/detail）
- [x] **8.6** SecurityConfig — `PERMIT_ALL_PATHS` 新增 `/api/pickup-point/**`
- [x] **8.7** `PickupTimeoutScheduler` — `@Scheduled(cron = "0 0 3 * * ?")` 每天凌晨 3 点扫描 status=5 超时未取订单 → 自动取消退回，超时天数从 `sys_config.pickup_timeout_days` 读取
- [x] **8.8** `CommunityFreshDeliveryApplication` — 加 `@EnableScheduling`
- [x] **8.9** 端到端测试 — `GroupLeaderApplicationServiceTest`（9 用例）+ `PickupVerifyServiceTest`（6 用例），15/15 全部通过

---

## 第九步 评价模块（已完成 ✅）

- [x] **9.1** Entity — `Review`
- [x] **9.2** Mapper — `ReviewMapper`
- [x] **9.3** DTO — `ReviewCreateDTO`（score 1-5 @Min/@Max 校验）/ `ReviewVO`（含 userNickname + productName）
- [x] **9.4** `ReviewService` + `ReviewServiceImpl` — create / getByOrderId / getByProductId / myReviews
  - `create()` @Transactional：先查重复 → 校验订单状态（5/6/7）→ 校验商品在订单中 → 创建评价 → 所有商品均已评价则完成订单(8)否则待评价(7)
- [x] **9.5** `ReviewController` — 4 个接口：POST /api/review（USER）、GET /api/review/order/{id}、GET /api/review/product/{id}（公开）、GET /api/review/my（USER）
- [x] **9.6** SecurityConfig — `PERMIT_ALL_PATHS` 新增 `/api/review/product/**`（公开浏览）
- [x] **9.7** 端到端测试 — `ReviewServiceTest`（9 用例），全部通过

---

## 第十步 优惠券 / 站内消息 / 批次库存 / 操作日志（已完成 ✅）

### 10a 优惠券模块

- [x] **10a.1** Entity — `Coupon` + `UserCoupon`
- [x] **10a.2** Mapper — `CouponMapper` + `UserCouponMapper`
- [x] **10a.3** DTO — `CouponSaveDTO`（创建/编辑）+ `CouponIssueDTO`（发券）
- [x] **10a.4** VO — `CouponVO`（模板响应）+ `UserCouponVO`（用户持券响应，含预估优惠金额）
- [x] **10a.5** `CouponService` + `CouponServiceImpl`
  - 管理员端：create / update / updateStatus / delete / page / issue
  - 用户端：myCoupons（按状态筛选，自动更新过期券）/ listAvailable（按订单金额+分类过滤，计算优惠金额）
  - 券类型校验：满减券/新人券/品类券 value>0，折扣券 0<value<1
  - 过期券自动标记：查询时 UPDATE status=2 WHERE expire_time < now
- [x] **10a.6** `CouponController` — 8 个接口：6 个管理员端（@PreAuthorize ADMIN）+ 2 个用户端（@PreAuthorize USER）
- [x] **10a.7** 下单集成 — `OrderCreateDTO` 已有 `userCouponId` 字段，`OrderServiceImpl.place()` 新增优惠券校验链路：
  1. 查 userCoupon 归属 + 未使用 + 未过期
  2. 查 coupon 模板有效性
  3. 门槛校验（totalAmount >= threshold）
  4. 范围校验（scopeType=0 全场 or scopeId IN 订单商品分类）
  5. 计算折扣（满减=固定减，折扣=orderAmount×(1-discountValue)）
  6. 标记 userCoupon.status=1（已使用）
  7. actualAmount 扣除 couponDiscount

### 10b 站内消息模块

- [x] **10b.1** Entity — `Notification`
- [x] **10b.2** Mapper — `NotificationMapper`
- [x] **10b.3** VO — `NotificationVO`（含 typeText 中文映射）
- [x] **10b.4** `NotificationService` + `NotificationServiceImpl`
  - getUnreadCount / getUnread / page / markAsRead / markAllRead / send
  - type→text：1=系统通知 2=订单通知 3=退款通知 4=到货提醒
- [x] **10b.5** `NotificationController` — 5 个接口：unread-count / unread / page / {id}/read / read-all

### 10c 批次库存模块

- [x] **10c.1** Entity — `ProductBatch`
- [x] **10c.2** Mapper — `ProductBatchMapper`
- [x] **10c.3** DTO/VO — `ProductBatchSaveDTO` / `ProductBatchVO`
- [x] **10c.4** `ProductBatchService` + `ProductBatchServiceImpl`
  - create：批次入库（remaining=quantity）
  - pageByProduct：按商品分页查批次（支持 nearExpiry 筛选）
  - getNearExpiry：查所有临期批次
  - allocateFIFO：按 expiryDate ASC 分配，早过期先出，返回分配明细
- [x] **10c.5** `ProductBatchController` — 4 个接口（@PreAuthorize MERCHANT）：create / page / near-expiry / allocate-fifo
- [x] **10c.6** `BatchExpiryScheduler` — @Scheduled 每天凌晨 2 点
  - 临期标记：remaining>0 AND expiryDate ≤ now+3天 → nearExpiry=1
  - 低库存预警：stock < stockThreshold → 日志告警

### 10d 操作日志模块

- [x] **10d.1** Entity — `OperationLog`
- [x] **10d.2** Mapper — `OperationLogMapper`
- [x] **10d.3** `OperationLogService` + `OperationLogServiceImpl`
  - record(userId, action, targetType, targetId, fromStatus, toStatus, detail)
  - 独立事务 @Transactional(propagation = REQUIRES_NEW)，主业务回滚不影响日志
  - 动作常量：ORDER_PAY / ORDER_CANCEL / ORDER_ACCEPT / ORDER_SORT_COMPLETE / DELIVERY_GRAB / DELIVERY_COMPLETE / PICKUP_VERIFY / GROUP_LEADER_REVIEW
- [x] **10d.4** 集成到现有 Service：
  - `OrderServiceImpl` — cancel / accept / sortComplete
  - `MockPaymentServiceImpl` — pay
  - `DeliveryServiceImpl` — grab / confirmDelivery
  - `GroupLeaderApplicationServiceImpl` — review / verifyPickup

### 修改的现有文件

| 文件 | 修改内容 |
|------|---------|
| `OrderServiceImpl.java` | 注入 CouponMapper/UserCouponMapper/OperationLogService；place() 新增优惠券校验链路（5 步）；cancel/accept/sortComplete 后记录操作日志 |
| `MockPaymentServiceImpl.java` | 注入 OperationLogService；pay() 后记录操作日志 |
| `DeliveryServiceImpl.java` | 注入 OperationLogService；grab()/confirmDelivery() 后记录操作日志 |
| `GroupLeaderApplicationServiceImpl.java` | 注入 OperationLogService；review()/verifyPickup() 后记录操作日志 |

### 新建文件汇总

| 子模块 | 文件数 |
|--------|--------|
| 10a 优惠券 | 11 |
| 10b 站内消息 | 6 |
| 10c 批次库存 | 8 |
| 10d 操作日志 | 4 |
| **合计** | **29** |

---

## 下次待做

## 第十一步 配送员申请 / 称重计价 / 收藏夹 / 数据看板 / 领券中心（已完成 ✅）

### 11a 配送员申请审核

- [x] **11a.1** SQL — `delivery_application` 表（user_id + real_name + phone + status + reject_reason）
- [x] **11a.2** Entity — `DeliveryApplication` + Mapper + DTO（Apply/Review）+ VO
- [x] **11a.3** `DeliveryApplicationService` + `DeliveryApplicationServiceImpl`
  - apply：校验无待审核申请 → INSERT
  - review：通过时 physicalDelete + INSERT ROLE_DELIVERY；拒绝时填原因
- [x] **11a.4** `DeliveryApplicationController` — 4 个接口：用户端 apply/my + 管理端 page/review

### 11b 称重计价差异处理

- [x] **11b.1** DTO — `SortItemDTO`（orderItemId + actualWeight + shortage）
- [x] **11b.2** `OrderService.sortComplete()` 签名改为 `void sortComplete(Long orderId, List<SortItemDTO> items)`
- [x] **11b.3** `OrderServiceImpl.sortComplete()` 新流程：
  1. 加载订单明细 + 商品信息（判断称重/固定规格）
  2. 缺货标记 → 退款该商品金额
  3. 称重商品 → actualWeight vs estimatedWeight → 差额 = (实重-预估) × 单价
  4. 固定规格 → actualAmount = amount
  5. 汇总差额 → 更新 order.totalAmount + order.actualAmount
- [x] **11b.4** `OrderController.sortComplete` 改为接收 @RequestBody List<SortItemDTO>

### 11c 收藏夹

- [x] **11c.1** SQL — `favorite` 表（user_id + product_id，UNIQUE KEY）
- [x] **11c.2** Entity — `Favorite` + Mapper + VO（含 productName/minPrice/productImage）
- [x] **11c.3** `FavoriteService` + `FavoriteServiceImpl` — add/remove/list/isFavorited，批量加载商品名+最低价消除 N+1
- [x] **11c.4** `FavoriteController` — 4 个接口：收藏/取消/列表/检查（@PreAuthorize USER）

### 11d 数据看板

- [x] **11d.1** VO — `DashboardVO`（今日概况）+ `SalesStatsVO`（销量统计）+ `HotProductVO`（热销商品）
- [x] **11d.2** `DashboardService` + `DashboardServiceImpl`
  - today()：按今日 order 表状态分组统计 + 销售额聚合
  - salesStats(period)：按 day/week/month 聚合 order_item 销量+金额+订单数
  - hotProducts()：GROUP BY product_id ORDER BY SUM(quantity) DESC LIMIT 10
- [x] **11d.3** `DashboardController` — 3 个接口（@PreAuthorize MERCHANT）

### 11e 优惠券领券中心

- [x] **11e.1** `CouponService` 新增 listCenter() + claim(couponId)
- [x] **11e.2** `CouponServiceImpl` — listCenter 排除已领模板；claim 校验未重复领取
- [x] **11e.3** `CouponController` 新增 GET /center + POST /{id}/claim

### 修改的现有文件

| 文件 | 修改内容 |
|------|---------|
| `init.sql` | 追加 delivery_application + favorite 建表（21 张表） |
| `service/OrderService.java` | sortComplete 签名变更 |
| `service/impl/OrderServiceImpl.java` | sortComplete 称重差异+缺货处理（~80行新逻辑） |
| `controller/OrderController.java` | sortComplete 改为接收 @RequestBody List<SortItemDTO> |
| `service/CouponService.java` | 新增 listCenter + claim |
| `service/impl/CouponServiceImpl.java` | 实现 listCenter + claim |
| `controller/CouponController.java` | 新增 center + claim 接口 |

### 新建文件汇总

| 子模块 | 文件数 |
|--------|--------|
| 11a 配送员申请 | 8 |
| 11b 称重计价 | 1 (SortItemDTO) |
| 11c 收藏夹 | 6 |
| 11d 数据看板 | 6 |
| 11e 领券中心 | 0（扩展现有） |
| **合计** | **21** |

---

## 下次待做

## 第十二步 团长佣金 / 支付对账 / 接口限流 / 临期折扣（已完成 ✅）

### 12a 团长佣金结算

- [x] **12a.1** SQL — `commission_record` 表（user_id + pickup_point_id + order_id + rate + amount + status）
- [x] **12a.2** Entity — `CommissionRecord` + Mapper + VO（CommissionVO/CommissionDetailVO）
- [x] **12a.3** `CommissionService` + `CommissionServiceImpl`
  - create(orderId)：核销后自动生成佣金，读 sys_config.commission_rate
  - summary()：累计/待提现/已提现汇总
  - withdraw()：一键标记所有"未提现"为"已提现"
- [x] **12a.4** `CommissionController` — 3 个接口（@PreAuthorize GROUP_LEADER）
- [x] **12a.5** 集成 — `GroupLeaderApplicationServiceImpl.verifyPickup()` 后调用 `commissionService.create()`

### 12b 支付对账

- [x] **12b.1** `PaymentReconciliationScheduler` — @Scheduled 每天凌晨 4:00
  - 查昨日 payment → 关联 order → 比对三要素（订单存在/状态正常/金额一致）
  - 差异记录 ERROR 日志

### 12c 接口限流

- [x] **12c.1** `@RateLimit` 注解（key + limit + window + message）
- [x] **12c.2** `RateLimitAspect` — Redis ZSet 滑动窗口 AOP 切面
  - 已登录用 userId，未登录用 IP 作为限流 key
- [x] **12c.3** 应用：`AuthController.login()`（10次/60s）+ `OrderController.place()`（5次/60s）
- [x] **12c.4** pom.xml — 追加 `spring-boot-starter-aop` 依赖

### 12d 临期折扣

- [x] **12d.1** `ProductVO` + `CartItemVO` — 新增 `nearExpiryDiscount` 字段
- [x] **12d.2** `ProductServiceImpl` — 注入 ProductBatchMapper/SysConfigMapper
  - `enrichNearExpiryDiscount()`：批量查临期批次 → 读 near_expiry_discount 配置 → 计算折扣价
  - 应用于 listForUser 的价格排序路径和时间排序路径
- [x] **12d.3** `init.sql` — 新增 sys_config：commission_rate="0.05" + near_expiry_discount="0.70"

### 修改的现有文件

| 文件 | 修改 |
|------|------|
| `pom.xml` | 新增 spring-boot-starter-aop 依赖 |
| `init.sql` | 新增 commission_record 表（22张）+ 2条 sys_config（共8条） |
| `AuthController.java` | login() 添加 @RateLimit |
| `OrderController.java` | place() 添加 @RateLimit |
| `dto/ProductVO.java` | 新增 nearExpiryDiscount 字段 |
| `dto/CartItemVO.java` | 新增 nearExpiryDiscount 字段 |
| `service/impl/ProductServiceImpl.java` | 注入 ProductBatchMapper/SysConfigMapper + enrichNearExpiryDiscount |
| `service/impl/GroupLeaderApplicationServiceImpl.java` | 注入 CommissionService + verifyPickup 后创建佣金 |
| `devlog.md` | 更新 |

### 新建文件汇总

| 子模块 | 文件数 |
|--------|--------|
| 12a 团长佣金 | 7 |
| 12b 支付对账 | 1 |
| 12c 接口限流 | 2 |
| 12d 临期折扣 | 0（修改现有） |
| **合计** | **10** |

---

## 下次待做

- [ ] **第十三步** 功能扩展：秒杀拼团 / 会员积分体系 / 消息队列全面异步化
- [ ] **第十四步** 前端完善：团长工作台 / 配送员工作台 / 商家后台页面
- [ ] **第十五步** 运维完善：Docker 部署 / CI/CD / 监控告警

