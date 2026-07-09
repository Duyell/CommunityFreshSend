package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <h2>今日概况响应</h2>
 *
 * <p>商家端首页数据看板 — 今日订单与营收概况.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class DashboardVO {

    /** 今日新下单数（status >= 1 的订单数） */
    private Long newOrders;

    /** 待接单数 */
    private Long pendingAccept;

    /** 待分拣数 */
    private Long pendingSorting;

    /** 待配送数 */
    private Long pendingDelivery;

    /** 配送中数 */
    private Long inDelivery;

    /** 今日完成数 */
    private Long completed;

    /** 今日销售额 */
    private BigDecimal todayRevenue;
}
