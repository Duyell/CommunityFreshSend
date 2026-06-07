package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>订单响应</h2>
 *
 * <p>含订单基本信息 + 状态文本 + 商品明细列表.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
@Builder
public class OrderVO {

    /** 订单ID */
    private Long id;

    /** 订单编号（22位） */
    private String orderNo;

    /** 下单用户ID */
    private Long userId;

    /** 收货地址ID */
    private Long addressId;

    /** 自提点ID */
    private Long pickupPointId;

    /** 配送方式：1=配送到家 2=送达自提点 */
    private Integer deliveryType;

    /** 配送时段 */
    private String deliveryTimeSlot;

    /** 订单状态码 */
    private Integer status;

    /** 订单状态文本（如"待付款"） */
    private String statusText;

    /** 商品总金额 */
    private BigDecimal totalAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 包装费 */
    private BigDecimal packageFee;

    /** 优惠券抵扣 */
    private BigDecimal couponDiscount;

    /** 实付金额 */
    private BigDecimal actualAmount;

    /** 自提码（6位数字，自提订单用） */
    private String pickupCode;

    /** 用户备注 */
    private String remark;

    /** 取消原因 */
    private String cancelReason;

    /** 支付时间 */
    private LocalDateTime paidTime;

    /** 送达/签收时间 */
    private LocalDateTime deliveredTime;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 订单商品明细 */
    private List<OrderItemVO> items;
}
