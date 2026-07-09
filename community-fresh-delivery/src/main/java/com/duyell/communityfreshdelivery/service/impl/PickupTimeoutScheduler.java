package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.OrderItem;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.OrderItemMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.ProductBatchMapper;
import com.duyell.communityfreshdelivery.mapper.UserCouponMapper;
import com.duyell.communityfreshdelivery.service.SysConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>自提超时退回调度器</h2>
 *
 * <p>每天凌晨 3 点将"已送达自提点"且超过 N 天未取的订单自动取消，
 * 并回补库存 + 释放优惠券.</p>
 *
 * <h3>配置</h3>
 * <p>超时天数由 {@code sys_config.pickup_timeout_days} 控制，默认 3 天.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PickupTimeoutScheduler {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductBatchMapper productBatchMapper;
    private final UserCouponMapper userCouponMapper;
    private final SysConfigService sysConfigService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private record BatchAlloc(long bid, int qty) {}

    /** 每天凌晨 3:00 执行 */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutPickupOrders() {
        log.info("开始扫描自提超时订单...");

        int timeoutDays = sysConfigService.getInt("pickup_timeout_days", 3);
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(timeoutDays);

        // 查询所有符合条件的订单（仅自提单）
        List<Order> timeoutOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.RECEIVED.getCode())
                        .eq(Order::getDeliveryType, 2) // 仅自提单
                        .le(Order::getUpdateTime, cutoffTime)
        );

        if (timeoutOrders.isEmpty()) {
            log.info("自提超时扫描完成：无超时订单");
            return;
        }

        int cancelledCount = 0;
        for (Order order : timeoutOrders) {
            try {
                cancelTimeoutOrder(order, timeoutDays);
                cancelledCount++;
            } catch (Exception e) {
                log.error("自提超时取消订单失败: orderId={} orderNo={}", order.getId(), order.getOrderNo(), e);
            }
        }

        log.info("自提超时扫描完成，共退回 {} 个订单 (cutoffTime={})", cancelledCount, cutoffTime);
    }

    /**
     * 取消单个自提超时订单：更新状态 + 回补库存 + 释放优惠券.
     */
    private void cancelTimeoutOrder(Order order, int timeoutDays) {
        // 原子更新订单状态（防并发：只有仍为 RECEIVED 的才更新）
        int updated = orderMapper.update(
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, order.getId())
                        .eq(Order::getStatus, OrderStatus.RECEIVED.getCode())
                        .set(Order::getStatus, OrderStatus.CANCELLED.getCode())
                        .set(Order::getCancelReason, "自提超时" + timeoutDays + "天未取，退回仓库")
        );

        if (updated == 0) {
            log.info("订单状态已变更，跳过: orderNo={}", order.getOrderNo());
            return;
        }

        // 回补批次库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
        );
        for (OrderItem item : items) {
            rollbackItemStock(item);
        }

        // 释放已使用的优惠券
        if (order.getCouponId() != null) {
            userCouponMapper.update(
                    new LambdaUpdateWrapper<com.duyell.communityfreshdelivery.entity.UserCoupon>()
                            .eq(com.duyell.communityfreshdelivery.entity.UserCoupon::getId, order.getCouponId())
                            .set(com.duyell.communityfreshdelivery.entity.UserCoupon::getStatus, 0)
            );
        }

        log.info("自提超时订单已取消: orderNo={} items={}", order.getOrderNo(), items.size());
    }

    /** 回补订单项库存（与 OrderServiceImpl.rollbackItemStock 逻辑一致） */
    private void rollbackItemStock(OrderItem item) {
        String allocJson = item.getBatchAllocations();
        if (allocJson != null && !allocJson.isBlank()) {
            try {
                List<BatchAlloc> allocs = OBJECT_MAPPER.readValue(
                        allocJson, new TypeReference<List<BatchAlloc>>() {});
                for (BatchAlloc a : allocs) {
                    productBatchMapper.addRemaining(a.bid(), a.qty());
                }
            } catch (Exception e) {
                log.error("解析批次分配明细失败: orderItemId={} json={}", item.getId(), allocJson, e);
            }
        } else if (item.getBatchId() != null) {
            productBatchMapper.addRemaining(item.getBatchId(), item.getQuantity());
        }
    }
}
