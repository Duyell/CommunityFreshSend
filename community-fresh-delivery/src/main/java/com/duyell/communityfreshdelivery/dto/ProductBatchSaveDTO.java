package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <h2>批次入库请求</h2>
 *
 * <p>商家新建批次，库存由 product_batch.remaining 统一管理.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
public class ProductBatchSaveDTO {

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 进货批次号 */
    @NotBlank(message = "批次号不能为空")
    private String batchNo;

    /** 进价 */
    @NotNull(message = "进价不能为空")
    private BigDecimal costPrice;

    /** 入库数量 */
    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量至少为1")
    private Integer quantity;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;
}
