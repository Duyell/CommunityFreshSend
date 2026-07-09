package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <h2>批次库存实体</h2>
 *
 * <p>对应 {@code product_batch} 表。同一商品可有多个批次（不同进货时间，不同过期日）.
 * 出库按 {@code expiry_date} 升序 FIFO 分配（早过期的先出）.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@TableName("product_batch")
public class ProductBatch {

    /** 批次ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 进货批次号 */
    private String batchNo;

    /** 进价 */
    private BigDecimal costPrice;

    /** 入库数量 */
    private Integer quantity;

    /** 剩余数量 */
    private Integer remaining;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;

    /** 临期标记：1=距过期≤3天 */
    private Integer nearExpiry;

    /** 入库时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
