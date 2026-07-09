package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>团长佣金记录实体</h2>
 *
 * <p>对应 {@code commission_record} 表。用户自提核销后自动生成佣金.
 * status：0=未提现 1=已提现.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@TableName("commission_record")
public class CommissionRecord {

    /** 佣金ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 团长用户ID */
    private Long userId;

    /** 自提点ID */
    private Long pickupPointId;

    /** 关联订单ID */
    private Long orderId;

    /** 订单金额 */
    private BigDecimal orderAmount;

    /** 佣金比例（如 0.05 = 5%） */
    private BigDecimal rate;

    /** 佣金金额 */
    private BigDecimal amount;

    /** 状态：0=未提现 1=已提现 */
    private Integer status;

    /** 提现时间 */
    private LocalDateTime withdrawTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
