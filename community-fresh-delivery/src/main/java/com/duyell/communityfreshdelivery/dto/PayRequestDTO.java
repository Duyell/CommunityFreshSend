package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <h2>支付请求</h2>
 *
 * <p>模拟支付场景下只需传订单ID.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
public class PayRequestDTO {

    /** 订单ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}
