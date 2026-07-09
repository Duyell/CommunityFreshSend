package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * <h2>加购请求</h2>
 *
 * <p>添加或修改购物车某 SKU 的数量.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
@Data
public class CartAddDTO {

    /** SKU ID */
    @NotNull(message = "SKU ID 不能为空")
    private Long skuId;

    /** 数量（1=加1件，修改时传目标数量，最多999件） */
    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须大于0")
    @jakarta.validation.constraints.Max(value = 999, message = "单次数量不能超过999")
    private Integer quantity;
}
