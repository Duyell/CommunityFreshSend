package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <h2>销量统计响应</h2>
 *
 * <p>按日/周/月聚合的商品销量与销售额.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class SalesStatsVO {

    /** 统计日期/周/月 */
    private String period;

    /** 销量（件数） */
    private Long totalQuantity;

    /** 销售额 */
    private BigDecimal totalAmount;

    /** 订单数 */
    private Long orderCount;
}
