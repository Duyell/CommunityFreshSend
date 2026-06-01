-- ============================================
-- 社区生鲜配送系统 · 数据库初始化脚本
-- ============================================
-- 规范约定：
--   1. 不使用外键及级联，数据完整性由应用层保证（阿里巴巴规范）
--   2. 所有表标配逻辑删除字段 deleted（MyBatis-Plus 自动处理）
--   3. 表名/字段名统一小写 + 下划线分隔
--   4. 必备字段：id（自增主键）、create_time、update_time、deleted
--   5. 金额字段统一 DECIMAL(10,2)，小数点后最多 2 位
--   6. 库存字段使用 INT（社区规模足够），不做超大数据量预留
-- ============================================

-- 建库
CREATE DATABASE IF NOT EXISTS community_fresh
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE community_fresh;

-- ============================================
-- 1. 用户模块
-- ============================================

-- 1.1 用户表
-- 角色说明：ROLE_USER 居民 / ROLE_DELIVERY 配送员 / ROLE_MERCHANT 商家 / ROLE_GROUP_LEADER 团长 / ROLE_ADMIN 管理员
-- 商家账号由管理员后台创建，不走注册；团长 = 普通用户 + 审核通过后追加 ROLE_GROUP_LEADER
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone`       VARCHAR(20)  NOT NULL                COMMENT '手机号（登录用户名）',
    `password`    VARCHAR(128) NOT NULL                COMMENT '密码（BCrypt 加密）',
    `nickname`    VARCHAR(50)  DEFAULT ''              COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT ''              COMMENT '头像 OSS Key',
    `status`      TINYINT      DEFAULT 1               COMMENT '账号状态：1=正常 0=禁用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除：1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.2 用户角色表
-- 一个用户可持有多个角色（如居民+团长），角色直接存字符串，不做角色字典表
CREATE TABLE `user_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID',
    `role`        VARCHAR(32)  NOT NULL                COMMENT '角色：ROLE_USER/ROLE_DELIVERY/ROLE_MERCHANT/ROLE_GROUP_LEADER/ROLE_ADMIN',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ============================================
-- 2. 收货地址
-- ============================================

CREATE TABLE `address` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID',
    `contact`     VARCHAR(20)  NOT NULL                COMMENT '收货人姓名',
    `phone`       VARCHAR(20)  NOT NULL                COMMENT '收货人电话',
    `province`    VARCHAR(20)  NOT NULL                COMMENT '省份',
    `city`        VARCHAR(20)  NOT NULL                COMMENT '城市',
    `district`    VARCHAR(20)  NOT NULL                COMMENT '区/县',
    `detail`      VARCHAR(255) NOT NULL                COMMENT '详细地址（门牌号）',
    `is_default`  TINYINT      DEFAULT 0               COMMENT '是否默认地址：1=默认',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ============================================
-- 3. 商品模块
-- ============================================

-- 3.1 商品分类表（树形结构，parent_id=0 为一级分类）
CREATE TABLE `category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id`   BIGINT       DEFAULT 0               COMMENT '父分类ID，0=一级分类',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '分类名称（如"热带水果"）',
    `sort`        INT          DEFAULT 0               COMMENT '排序（越小越前）',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态：1=启用 0=停用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 3.2 商品表
-- 库存字段在 MySQL 中维护，Redis 缓存读多写少场景
CREATE TABLE `product` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `category_id` BIGINT        NOT NULL                COMMENT '所属分类ID',
    `name`        VARCHAR(100)  NOT NULL                COMMENT '商品名称',
    `description` VARCHAR(500)  DEFAULT ''              COMMENT '商品描述',
    `images`      VARCHAR(1024) DEFAULT ''              COMMENT '商品图片 OSS Key 列表（JSON 数组字符串）',
    `status`      TINYINT       DEFAULT 1               COMMENT '状态：1=上架 2=下架 3=售罄',
    `is_weighted` TINYINT       DEFAULT 0               COMMENT '计价方式：0=固定规格 1=称重计价',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `deleted`     TINYINT       DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_name` (`name`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 3.3 商品规格表（SKU）
-- 固定规格：盒装草莓 300g/盒 ¥19.9，库存按"盒"计
-- 称重计价：散装苹果 ¥8/斤，库存按"斤"计，下单按预估重量，分拣后按实重多退少补
CREATE TABLE `product_sku` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    `product_id`  BIGINT        NOT NULL                COMMENT '商品ID',
    `spec_name`   VARCHAR(100)  DEFAULT '默认规格'       COMMENT '规格名称（如"300g/盒"、"散称"）',
    `price`       DECIMAL(10,2) NOT NULL                COMMENT '售价',
    `stock`       INT           DEFAULT 0               COMMENT '当前库存',
    `stock_threshold` INT       DEFAULT 10              COMMENT '库存预警阈值',
    `status`      TINYINT       DEFAULT 1               COMMENT '状态：1=启用 0=停用',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`     TINYINT       DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表';

-- ============================================
-- 4. 批次库存（Phase 2 — 预建表）
-- ============================================

CREATE TABLE `product_batch` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '批次ID',
    `product_id`      BIGINT        NOT NULL                COMMENT '商品ID',
    `batch_no`        VARCHAR(64)   NOT NULL                COMMENT '进货批次号',
    `cost_price`      DECIMAL(10,2) NOT NULL                COMMENT '进价',
    `quantity`        INT           NOT NULL                COMMENT '入库数量',
    `remaining`       INT           NOT NULL                COMMENT '剩余数量',
    `production_date` DATE          DEFAULT NULL            COMMENT '生产日期',
    `expiry_date`     DATE          DEFAULT NULL            COMMENT '过期日期',
    `near_expiry`     TINYINT       DEFAULT 0               COMMENT '临期标记：1=距过期≤3天',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`         TINYINT       DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_expiry_date` (`expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次库存表';

-- ============================================
-- 5. 订单模块
-- ============================================

-- 5.1 订单表
-- 订单状态机详见 function.md → 订单状态流转
-- 配送方式：1=配送到家 2=送达自提点
CREATE TABLE `order` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`            VARCHAR(32)   NOT NULL                COMMENT '订单编号：14位时间戳(yyyyMMddHHmmss) + 8位随机码（大小写+数字），共22位',
    `user_id`             BIGINT        NOT NULL                COMMENT '下单用户ID',
    `address_id`          BIGINT        DEFAULT NULL            COMMENT '收货地址ID（配送到家）',
    `pickup_point_id`     BIGINT        DEFAULT NULL            COMMENT '自提点ID（送达自提点）',
    `delivery_type`       TINYINT       NOT NULL                COMMENT '配送方式：1=配送到家 2=送达自提点',
    `delivery_time_slot`  VARCHAR(20)   DEFAULT ''              COMMENT '配送时段：上午(9-12)/下午(14-17)/晚间(17-20)',
    `status`              TINYINT       NOT NULL DEFAULT 0      COMMENT '订单状态：0=待付款 1=待接单 2=待分拣 3=待配送 4=配送中 5=已签收/已送达自提点 6=用户已自提 7=待评价 8=已完成 9=已取消 10=退款中 11=已退款',
    `total_amount`        DECIMAL(10,2) NOT NULL                COMMENT '订单总金额（商品合计）',
    `delivery_fee`        DECIMAL(10,2) DEFAULT 0.00            COMMENT '配送费',
    `package_fee`         DECIMAL(10,2) DEFAULT 0.00            COMMENT '包装费',
    `coupon_discount`     DECIMAL(10,2) DEFAULT 0.00            COMMENT '优惠券抵扣金额',
    `actual_amount`       DECIMAL(10,2) NOT NULL                COMMENT '实付金额（total+d+pack-coupon）',
    `coupon_id`           BIGINT        DEFAULT NULL            COMMENT '使用的优惠券记录ID（user_coupon.id）',
    `remark`              VARCHAR(255)  DEFAULT ''              COMMENT '用户备注',
    `cancel_reason`       VARCHAR(255)  DEFAULT ''              COMMENT '取消原因',
    `paid_time`           DATETIME      DEFAULT NULL            COMMENT '支付时间',
    `delivered_time`      DATETIME      DEFAULT NULL            COMMENT '送达/签收时间',
    `pickup_code`         VARCHAR(6)    DEFAULT ''              COMMENT '自提取货码（6位数字）',
    `pickup_time`         DATETIME      DEFAULT NULL            COMMENT '用户自提核销时间',
    `create_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `deleted`             TINYINT       DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status_create_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 5.2 订单明细表
-- 下单时记录商品当时的售价、预估重量（称重商品），分拣后更新实重
CREATE TABLE `order_item` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`        BIGINT        NOT NULL                COMMENT '订单ID',
    `product_id`      BIGINT        NOT NULL                COMMENT '商品ID',
    `sku_id`          BIGINT        NOT NULL                COMMENT '规格ID',
    `product_name`    VARCHAR(100)  NOT NULL                COMMENT '商品名称快照',
    `spec_name`       VARCHAR(100)  DEFAULT ''              COMMENT '规格名称快照',
    `price`           DECIMAL(10,2) NOT NULL                COMMENT '下单时单价',
    `quantity`        INT           NOT NULL                COMMENT '下单数量',
    `estimated_weight` DECIMAL(10,2) DEFAULT 0.00           COMMENT '预估重量（斤，称重商品用）',
    `actual_weight`   DECIMAL(10,2) DEFAULT 0.00            COMMENT '分拣实重（斤，称重商品用）',
    `amount`          DECIMAL(10,2) NOT NULL                COMMENT '小计金额（预估）',
    `actual_amount`   DECIMAL(10,2) DEFAULT 0.00            COMMENT '分拣后调整金额（实重×单价）',
    `shortage`        TINYINT       DEFAULT 0               COMMENT '缺货标记：1=缺货',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`         TINYINT       DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 5.3 支付流水表
-- 模拟支付模式：用户点击"已支付"即生成一条支付成功记录
CREATE TABLE `payment` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '支付流水ID',
    `order_id`        BIGINT        NOT NULL                COMMENT '订单ID',
    `pay_no`          VARCHAR(64)   NOT NULL                COMMENT '支付流水号（幂等键）',
    `amount`          DECIMAL(10,2) NOT NULL                COMMENT '支付金额',
    `method`          TINYINT       DEFAULT 1               COMMENT '支付方式：1=模拟支付 2=微信 3=支付宝',
    `status`          TINYINT       NOT NULL                COMMENT '状态：1=支付成功 2=已退款',
    `paid_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '支付时间',
    `refund_time`     DATETIME      DEFAULT NULL            COMMENT '退款时间',
    `refund_amount`   DECIMAL(10,2) DEFAULT 0.00            COMMENT '退款金额',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`         TINYINT       DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_no` (`pay_no`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ============================================
-- 6. 配送模块
-- ============================================

CREATE TABLE `delivery` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '配送记录ID',
    `order_id`        BIGINT      NOT NULL                COMMENT '订单ID',
    `delivery_user_id` BIGINT     DEFAULT NULL            COMMENT '配送员用户ID',
    `status`          TINYINT     NOT NULL                COMMENT '状态：1=待取货 2=配送中 3=已送达',
    `pickup_time`     DATETIME    DEFAULT NULL            COMMENT '取货确认时间',
    `deliver_time`    DATETIME    DEFAULT NULL            COMMENT '送达确认时间',
    `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`         TINYINT     DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_delivery_user_id` (`delivery_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配送记录表';

-- ============================================
-- 7. 自提模块
-- ============================================

-- 7.1 自提点表
-- owner_type：1=平台自营 2=团长自提点
-- owner_type=1 时 owner_id 为 NULL（平台管理）
-- owner_type=2 时 owner_id 关联 user 表的团长用户ID
CREATE TABLE `pickup_point` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自提点ID',
    `name`         VARCHAR(100) NOT NULL                COMMENT '自提点名称（如"阳光花园-东门"）',
    `contact`      VARCHAR(20)  NOT NULL                COMMENT '联系人',
    `phone`        VARCHAR(20)  NOT NULL                COMMENT '联系电话',
    `address`      VARCHAR(255) NOT NULL                COMMENT '自提点地址',
    `owner_type`   TINYINT      DEFAULT 1               COMMENT '归属类型：1=平台自营 2=团长',
    `owner_id`     BIGINT       DEFAULT NULL            COMMENT '团长用户ID（owner_type=2时有值）',
    `status`       TINYINT      DEFAULT 1               COMMENT '状态：1=营业 0=停用',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`      TINYINT      DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自提点表';

-- 7.2 团长申请表
-- 普通用户提交团长申请 → 管理员审核 → 通过后创建 pickup_point + 追加 ROLE_GROUP_LEADER
CREATE TABLE `group_leader_application` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `user_id`       BIGINT       NOT NULL                COMMENT '申请人用户ID',
    `address`       VARCHAR(255) NOT NULL                COMMENT '自提点地址（小区名+门牌号）',
    `contact_name`  VARCHAR(20)  NOT NULL                COMMENT '联系人姓名',
    `contact_phone` VARCHAR(20)  NOT NULL                COMMENT '联系电话',
    `remark`        VARCHAR(255) DEFAULT ''              COMMENT '附言',
    `status`        TINYINT      DEFAULT 0               COMMENT '审核状态：0=待审核 1=已通过 2=已拒绝',
    `reject_reason` VARCHAR(255) DEFAULT ''              COMMENT '拒绝原因',
    `reviewed_time` DATETIME     DEFAULT NULL            COMMENT '审核时间',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`       TINYINT      DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团长申请表';

-- ============================================
-- 8. 评价模块
-- ============================================

CREATE TABLE `review` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id`    BIGINT        NOT NULL                COMMENT '订单ID',
    `user_id`     BIGINT        NOT NULL                COMMENT '评价用户ID',
    `product_id`  BIGINT        NOT NULL                COMMENT '被评价商品ID',
    `freshness`   TINYINT       NOT NULL                COMMENT '新鲜度评分（1-5星）',
    `match_score` TINYINT       NOT NULL                COMMENT '描述相符评分（1-5星）',
    `content`     VARCHAR(500)  DEFAULT ''              COMMENT '文字评价',
    `images`      VARCHAR(1024) DEFAULT ''              COMMENT '评价图片 OSS Key 列表（JSON 数组）',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    `deleted`     TINYINT       DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ============================================
-- 9. 优惠券模块
-- ============================================

-- 9.1 优惠券模板
-- type：1=满减券 2=折扣券 3=新人券 4=品类券
CREATE TABLE `coupon` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '券模板ID',
    `name`           VARCHAR(100)  NOT NULL                COMMENT '券名称（如"满50减5"）',
    `type`           TINYINT       NOT NULL                COMMENT '券类型：1=满减券 2=折扣券 3=新人券 4=品类券',
    `threshold`      DECIMAL(10,2) DEFAULT 0.00            COMMENT '使用门槛（满减券/折扣券的最低金额）',
    `discount_value` DECIMAL(10,2) NOT NULL                COMMENT '优惠值（满减=减多少钱，折扣=0.8表示8折）',
    `scope_type`     TINYINT       DEFAULT 0               COMMENT '适用范围：0=全场 1=指定分类',
    `scope_id`       BIGINT        DEFAULT NULL            COMMENT '适用分类ID（scope_type=1时有值）',
    `valid_days`     INT           NOT NULL                COMMENT '有效期天数（领取后N天内有效）',
    `status`         TINYINT       DEFAULT 1               COMMENT '状态：1=启用 0=停用',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`        TINYINT       DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- 9.2 用户持有优惠券
CREATE TABLE `user_coupon` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '持有记录ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
    `coupon_id`   BIGINT   NOT NULL                COMMENT '券模板ID',
    `status`      TINYINT  DEFAULT 0               COMMENT '状态：0=未使用 1=已使用 2=已过期',
    `expire_time` DATETIME NOT NULL                COMMENT '过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    `deleted`     TINYINT  DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持有优惠券表';

-- ============================================
-- 10. 站内消息
-- ============================================

CREATE TABLE `notification` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id`     BIGINT       NOT NULL                COMMENT '接收用户ID',
    `title`       VARCHAR(100) NOT NULL                COMMENT '消息标题',
    `content`     VARCHAR(500) NOT NULL                COMMENT '消息内容',
    `type`        TINYINT      NOT NULL                COMMENT '消息类型：1=系统通知 2=订单通知 3=退款通知 4=到货提醒',
    `is_read`     TINYINT      DEFAULT 0               COMMENT '是否已读：0=未读 1=已读',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id_is_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- ============================================
-- 11. 系统配置
-- ============================================

CREATE TABLE `sys_config` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key`  VARCHAR(64)  NOT NULL                COMMENT '配置键（如 delivery_fee）',
    `config_value` VARCHAR(255) NOT NULL               COMMENT '配置值',
    `description` VARCHAR(255) DEFAULT ''              COMMENT '配置说明',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ============================================
-- 12. 操作日志
-- ============================================

CREATE TABLE `operation_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`     BIGINT       DEFAULT NULL            COMMENT '操作人用户ID（系统操作时为NULL）',
    `action`      VARCHAR(50)  NOT NULL                COMMENT '操作动作（如 ORDER_CANCEL、REVIEW_APPROVE）',
    `target_type` VARCHAR(50)  DEFAULT ''              COMMENT '操作对象类型（如 ORDER、USER）',
    `target_id`   BIGINT       DEFAULT NULL            COMMENT '操作对象ID',
    `from_status` VARCHAR(20)  DEFAULT ''              COMMENT '变更前状态',
    `to_status`   VARCHAR(20)  DEFAULT ''              COMMENT '变更后状态',
    `detail`      VARCHAR(500) DEFAULT ''              COMMENT '操作详情',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================
-- 初始数据
-- ============================================

-- 测试用户（密码明文均为 "123456"，hash 由 BCryptPasswordEncoder 生成后手动填入）
-- 当前为占位 hash，部署时替换为真实 BCrypt 密文
INSERT INTO `user` (`id`, `phone`, `password`, `nickname`) VALUES
(1, '13800000001', '$2a$10$PLACEHOLDER_HASH_REPLACE_AFTER_FIRST_RUN', '测试居民'),
(2, '13800000002', '$2a$10$PLACEHOLDER_HASH_REPLACE_AFTER_FIRST_RUN', '测试配送员'),
(3, '13800000003', '$2a$10$PLACEHOLDER_HASH_REPLACE_AFTER_FIRST_RUN', '测试商家'),
(4, '13800000004', '$2a$10$PLACEHOLDER_HASH_REPLACE_AFTER_FIRST_RUN', '测试管理员');

-- 用户角色
INSERT INTO `user_role` (`user_id`, `role`) VALUES
(1, 'ROLE_USER'),
(2, 'ROLE_USER'), (2, 'ROLE_DELIVERY'),
(3, 'ROLE_MERCHANT'),
(4, 'ROLE_ADMIN');

-- 系统配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`) VALUES
('delivery_fee', '5.00', '基础配送费（元）'),
('free_delivery_threshold', '30.00', '满额免配送费门槛（元）'),
('min_order_amount', '15.00', '起送价（元）'),
('package_fee', '1.00', '包装费（元/单）'),
('order_timeout', '15', '订单超时自动取消（分钟）'),
('pickup_timeout_days', '3', '自提超时未取退回天数');
