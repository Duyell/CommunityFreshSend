package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>商品规格实体（SKU）</h2>
 *
 * <p>对应 {@code product_sku} 表。一个商品可配多个规格，
 * 每个规格有独立的价格和库存.
 * 固定规格（如 300g/盒）和称重计价（如散称 ¥8/斤）均通过此表存储.</p>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Data
@TableName("product_sku")
public class ProductSku {

    /** SKU ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商品 ID */
    private Long productId;

    /** 规格名称（如"300g/盒"、"散称"） */
    private String specName;

    /** 售价 */
    private BigDecimal price;

    /** 当前库存 */
    private Integer stock;

    /** 库存预警阈值 */
    private Integer stockThreshold;

    /** 状态：1=启用 0=停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
