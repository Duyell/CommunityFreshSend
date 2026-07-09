package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>佣金明细响应</h2>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class CommissionDetailVO {

    /** 佣金ID */
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 订单金额 */
    private BigDecimal orderAmount;

    /** 佣金比例 */
    private BigDecimal rate;

    /** 佣金金额 */
    private BigDecimal amount;

    /** 状态：0=未提现 1=已提现 */
    private Integer status;

    /** 状态文本 */
    private String statusText;

    /** 提现时间 */
    private LocalDateTime withdrawTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
