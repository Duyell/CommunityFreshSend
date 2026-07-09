package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <h2>热销商品响应</h2>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class HotProductVO {

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 销量（件数） */
    private Long totalQuantity;

    /** 销售额 */
    private BigDecimal totalAmount;
}
