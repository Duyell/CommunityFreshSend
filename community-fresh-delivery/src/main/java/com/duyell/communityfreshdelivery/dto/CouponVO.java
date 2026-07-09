package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>优惠券模板响应</h2>
 *
 * <p>供管理员查看券模板列表.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class CouponVO {

    /** 券模板ID */
    private Long id;

    /** 券名称 */
    private String name;

    /** 券类型：1=满减券 2=折扣券 3=新人券 4=品类券 */
    private Integer type;

    /** 券类型文本 */
    private String typeText;

    /** 使用门槛金额 */
    private BigDecimal threshold;

    /** 优惠值 */
    private BigDecimal discountValue;

    /** 适用范围：0=全场 1=指定分类 */
    private Integer scopeType;

    /** 适用分类ID */
    private Long scopeId;

    /** 有效期天数 */
    private Integer validDays;

    /** 状态：1=启用 0=停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
