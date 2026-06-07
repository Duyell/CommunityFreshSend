package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>支付流水实体</h2>
 *
 * <p>对应 {@code payment} 表。当前为模拟支付模式（method=1），
 * 后续可扩展微信/支付宝等真实支付渠道.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
@TableName("payment")
public class Payment {

    /** 支付流水ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 支付流水号（幂等键，唯一索引） */
    private String payNo;

    /** 支付金额 */
    private BigDecimal amount;

    /** 支付方式：1=模拟支付 2=微信 3=支付宝 */
    private Integer method;

    /** 状态：1=支付成功 2=已退款 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime paidTime;

    /** 退款时间 */
    private LocalDateTime refundTime;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
