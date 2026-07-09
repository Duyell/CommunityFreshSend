package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>商品响应</h2>
 *
 * <p>用于商品详情和分页列表。列表场景 SKU 不填充（{@code skus=null}），
 * 详情场景包含完整 SKU 列表.</p>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Data
@Builder
public class ProductVO {

    /** 商品 ID */
    private Long id;

    /** 所属分类 ID */
    private Long categoryId;

    /** 商品名称 */
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品图片列表 */
    private String images;

    /** 状态：1=上架 2=下架 3=售罄 */
    private Integer status;

    /** 计价方式：0=固定规格 1=称重计价 */
    private Integer isWeighted;

    /** 最低售价（列表页展示用，取 SKU 中最小的 price） */
    private BigDecimal minPrice;

    /** 总库存（从 product_batch 汇总，列表页展示"仅剩X件"） */
    private Integer totalStock;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 临期折扣价（仅临期商品的列表页有值，null=无临期折扣） */
    private BigDecimal nearExpiryDiscount;

    /** 规格列表（列表页为 null） */
    private List<SkuVO> skus;

    /**
     * <h2>规格响应</h2>
     */
    @Data
    @Builder
    public static class SkuVO {
        /** SKU ID */
        private Long id;
        /** 规格名称 */
        private String specName;
        /** 售价 */
        private BigDecimal price;
    }
}
