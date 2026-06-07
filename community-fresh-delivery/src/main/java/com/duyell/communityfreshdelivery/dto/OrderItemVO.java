package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <h2>订单明细响应</h2>
 *
 * <p>下单时快照的商品信息，用于订单详情页展示.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
@Builder
public class OrderItemVO {

    /** 明细ID */
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 规格ID */
    private Long skuId;

    /** 商品名称（快照） */
    private String productName;

    /** 规格名称（快照） */
    private String specName;

    /** 商品图片 */
    private String productImage;

    /** 下单时单价 */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 */
    private BigDecimal amount;

    /** 缺货标记 */
    private Boolean shortage;
}
