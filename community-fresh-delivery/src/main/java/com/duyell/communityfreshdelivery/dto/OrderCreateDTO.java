package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <h2>下单请求</h2>
 *
 * <p>配送方式区分配送到家（需 addressId）和自提（需 pickupPointId），
 * 业务层校验二者必传其一.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
public class OrderCreateDTO {

    /** 收货地址ID（配送到家时必填） */
    private Long addressId;

    /** 自提点ID（送达自提点时必填） */
    private Long pickupPointId;

    /** 配送方式：1=配送到家 2=送达自提点 */
    @NotNull(message = "配送方式不能为空")
    private Integer deliveryType;

    /** 配送时段：上午(9-12) / 下午(14-17) / 晚间(17-20) */
    private String deliveryTimeSlot;

    /** 用户备注（可选） */
    private String remark;

    /** 使用的优惠券记录ID（Phase 2，预留） */
    private Long userCouponId;
}
