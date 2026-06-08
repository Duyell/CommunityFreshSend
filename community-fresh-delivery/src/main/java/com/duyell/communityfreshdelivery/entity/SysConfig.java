package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>系统配置实体</h2>
 *
 * <p>对应 {@code sys_config} 表。存储起送价/配送费/超时时间等业务参数，
 * 运行时通过 {@code config_key} 读取，支持动态修改.</p>
 *
 * <h3>已有配置项</h3>
 * <ul>
 *   <li>{@code delivery_fee} — 基础配送费</li>
 *   <li>{@code free_delivery_threshold} — 满额免配送费门槛</li>
 *   <li>{@code min_order_amount} — 起送价</li>
 *   <li>{@code package_fee} — 包装费</li>
 *   <li>{@code order_timeout} — 订单超时自动取消（分钟）</li>
 *   <li>{@code pickup_timeout_days} — 自提超时未取退回天数</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Data
@TableName("sys_config")
public class SysConfig {

    /** 配置ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键（唯一，如 delivery_fee） */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置说明 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
