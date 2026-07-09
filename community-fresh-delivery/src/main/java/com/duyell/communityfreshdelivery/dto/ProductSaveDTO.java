package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * <h2>商品创建/编辑请求</h2>
 *
 * <p>同时包含商品主信息和 SKU 列表，由 {@code ProductService} 在事务中一次处理.
 * 编辑时 SKU 采用"先删后插"策略，前端需传完整的 SKU 列表.</p>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Data
public class ProductSaveDTO {

    /** 所属分类 ID */
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 商品名称 */
    @NotBlank(message = "商品名称不能为空")
    private String name;

    /** 商品描述 */
    private String description;

    /** 商品图片 OSS Key 列表（JSON 数组字符串） */
    private String images;

    /** 计价方式：0=固定规格 1=称重计价 */
    private Integer isWeighted;

    /** SKU 列表（至少一个） */
    @NotEmpty(message = "至少需要一个规格")
    @Valid
    private List<SkuItem> skus;

    /**
     * <h2>规格项</h2>
     */
    @Data
    public static class SkuItem {

        /** 规格名称（如"300g/盒"） */
        @NotBlank(message = "规格名称不能为空")
        private String specName;

        /** 售价 */
        @NotNull(message = "售价不能为空")
        @Positive(message = "售价必须大于0")
        private BigDecimal price;
    }
}
