package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <h2>购物车列表项</h2>
 *
 * <p>由 Redis 中的 skuId+quantity 拼接 ProductSku/Product 信息组装而成.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
@Data
@Builder
public class CartItemVO {

    /** SKU ID */
    private Long skuId;

    /** 商品 ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品图片（第一张） */
    private String productImage;

    /** 规格名称（如"300g/盒"） */
    private String specName;

    /** 单价 */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 当前库存（前端用于显示"仅剩X件"） */
    private Integer stock;

    /** 库存是否充足：true=库存足够 false=库存不足 */
    private Boolean stockSufficient;
}
