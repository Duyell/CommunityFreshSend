package com.duyell.communityfreshdelivery.service;

/**
 * <h2>操作日志服务</h2>
 *
 * <p>记录订单状态变更、审核操作等关键业务动作.
 * 方法为 void，独立事务（REQUIRES_NEW），不影响主业务流程.</p>
 *
 * <h3>操作动作常量</h3>
 * <pre>{@code
 * ORDER_PAY            — 支付成功
 * ORDER_CANCEL         — 取消订单
 * ORDER_ACCEPT         — 商家接单
 * ORDER_SORT_COMPLETE  — 分拣完成
 * DELIVERY_GRAB        — 配送员抢单
 * DELIVERY_COMPLETE    — 配送完成
 * PICKUP_VERIFY        — 自提核销
 * GROUP_LEADER_REVIEW  — 团长审核
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-09
 */
public interface OperationLogService {

    // ==================== 操作动作常量 ====================

    String ORDER_PAY = "ORDER_PAY";
    String ORDER_CANCEL = "ORDER_CANCEL";
    String ORDER_ACCEPT = "ORDER_ACCEPT";
    String ORDER_SORT_COMPLETE = "ORDER_SORT_COMPLETE";
    String DELIVERY_GRAB = "DELIVERY_GRAB";
    String DELIVERY_COMPLETE = "DELIVERY_COMPLETE";
    String PICKUP_VERIFY = "PICKUP_VERIFY";
    String GROUP_LEADER_REVIEW = "GROUP_LEADER_REVIEW";

    String TARGET_ORDER = "ORDER";
    String TARGET_APPLICATION = "GROUP_LEADER_APPLICATION";

    // ==================== 方法 ====================

    /**
     * 记录操作日志.
     *
     * @param userId     操作人用户ID
     * @param action     操作动作（使用本接口常量）
     * @param targetType 操作对象类型（使用本接口常量）
     * @param targetId   操作对象ID
     * @param fromStatus 变更前状态
     * @param toStatus   变更后状态
     * @param detail     操作详情
     */
    void record(Long userId, String action, String targetType, Long targetId,
                String fromStatus, String toStatus, String detail);
}
