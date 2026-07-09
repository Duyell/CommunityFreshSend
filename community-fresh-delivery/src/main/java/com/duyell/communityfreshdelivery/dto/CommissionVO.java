package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <h2>团长佣金概览响应</h2>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class CommissionVO {

    /** 累计佣金 */
    private BigDecimal totalCommission;

    /** 待提现 */
    private BigDecimal pendingWithdraw;

    /** 已提现 */
    private BigDecimal withdrawn;
}
