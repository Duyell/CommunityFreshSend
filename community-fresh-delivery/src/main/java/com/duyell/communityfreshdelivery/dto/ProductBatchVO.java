package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <h2>批次库存响应</h2>
 *
 * <p>含商品名称，供前端展示.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class ProductBatchVO {

    /** 批次ID */
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

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
}
