package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <h2>优惠券模板创建/编辑请求</h2>
 *
 * <p>管理员创建或编辑优惠券模板.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
public class CouponSaveDTO {

    /** 券名称 */
    @NotBlank(message = "券名称不能为空")
    private String name;

    /** 券类型：1=满减券 2=折扣券 3=新人券 4=品类券 */
    @NotNull(message = "券类型不能为空")
    private Integer type;

    /** 使用门槛金额 */
    private BigDecimal threshold;

    /** 优惠值（满减=减多少钱，折扣=0.8表示8折） */
    @NotNull(message = "优惠值不能为空")
    @DecimalMin(value = "0.01", message = "优惠值必须大于0")
    private BigDecimal discountValue;

    /** 适用范围：0=全场 1=指定分类 */
    private Integer scopeType;

    /** 适用分类ID（scopeType=1时有值） */
    private Long scopeId;

    /** 有效期天数 */
    @NotNull(message = "有效期不能为空")
    @Min(value = 1, message = "有效期至少1天")
    private Integer validDays;
}
