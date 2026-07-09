package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <h2>优惠券发放请求</h2>
 *
 * <p>管理员给指定用户发放优惠券.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
public class CouponIssueDTO {

    /** 接收用户ID */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 券模板ID */
    @NotNull(message = "券模板ID不能为空")
    private Long couponId;
}
