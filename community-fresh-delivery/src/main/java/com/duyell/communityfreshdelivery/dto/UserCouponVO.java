package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>用户持有优惠券响应</h2>
 *
 * <p>含券模板信息 + 使用状态 + 过期时间 + 预估可优惠金额.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class UserCouponVO {

    /** 持有记录ID */
    private Long id;

    /** 券模板ID */
    private Long couponId;

    /** 券名称 */
    private String couponName;

    /** 券类型：1=满减券 2=折扣券 3=新人券 4=品类券 */
    private Integer type;

    /** 券类型文本 */
    private String typeText;

    /** 适用范围：0=全场 1=指定分类 */
    private Integer scopeType;

    /** 适用分类ID */
    private Long scopeId;

    /** 使用门槛 */
    private BigDecimal threshold;

    /** 优惠值 */
    private BigDecimal discountValue;

    /** 预估可优惠金额（传入订单金额时计算，否则为原始值） */
    private BigDecimal discountAmount;

    /** 状态：0=未使用 1=已使用 2=已过期 */
    private Integer status;

    /** 状态文本 */
    private String statusText;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 领取时间 */
    private LocalDateTime createTime;
}
