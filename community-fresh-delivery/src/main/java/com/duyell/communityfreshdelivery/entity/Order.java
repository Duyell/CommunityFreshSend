package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>订单实体</h2>
 *
 * <p>对应 {@code order} 表。关联用户/收货地址/自提点/优惠券，
 * 单号在应用层生成（14位时间戳+8位随机码），配送方式区分到家/自提.</p>
 *
 * <h3>订单状态说明</h3>
 * <pre>{@code
 * 0=待付款  1=待接单  2=待分拣  3=待配送  4=配送中
 * 5=已签收/已送达自提点  6=用户已自提  7=待评价  8=已完成
 * 9=已取消  10=退款中  11=已退款
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
@TableName("`order`")
public class Order {

    /** 订单ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号（14位时间戳+8位随机码，共22位） */
    private String orderNo;

    /** 下单用户ID */
    private Long userId;

    /** 收货地址ID（配送到家时有值） */
    private Long addressId;

    /** 自提点ID（送达自提点时有值） */
    private Long pickupPointId;

    /** 配送方式：1=配送到家 2=送达自提点 */
    private Integer deliveryType;

    /** 配送时段：上午(9-12)/下午(14-17)/晚间(17-20) */
    private String deliveryTimeSlot;

    /**
     * 订单状态：
     * 0=待付款 1=待接单 2=待分拣 3=待配送 4=配送中
     * 5=已签收/已送达自提点 6=用户已自提 7=待评价 8=已完成
     * 9=已取消 10=退款中 11=已退款
     */
    private Integer status;

    /** 订单商品总金额 */
    private BigDecimal totalAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 包装费 */
    private BigDecimal packageFee;

    /** 优惠券抵扣金额 */
    private BigDecimal couponDiscount;

    /** 实付金额（totalAmount + deliveryFee + packageFee - couponDiscount） */
    private BigDecimal actualAmount;

    /** 使用的优惠券记录ID（user_coupon.id） */
    private Long couponId;

    /** 用户备注 */
    private String remark;

    /** 取消原因 */
    private String cancelReason;

    /** 支付时间 */
    private LocalDateTime paidTime;

    /** 送达/签收时间 */
    private LocalDateTime deliveredTime;

    /** 自提取货码（6位数字，自提订单用） */
    private String pickupCode;

    /** 用户自提核销时间 */
    private LocalDateTime pickupTime;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
