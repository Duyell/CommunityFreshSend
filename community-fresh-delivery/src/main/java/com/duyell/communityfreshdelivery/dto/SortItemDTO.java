package com.duyell.communityfreshdelivery.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * <h2>分拣明细请求</h2>
 *
 * <p>商家分拣时逐项确认：称重商品填实重，缺货商品标 shortage.
 * 固定规格商品只传 skuId + shortage 即可.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
public class SortItemDTO {

    /** 订单明细ID (orderItem.id) */
    private Long orderItemId;

    /** 分拣实重（斤，仅称重商品需要填） */
    private BigDecimal actualWeight;

    /** 缺货标记：true=缺货 */
    private Boolean shortage;
}
