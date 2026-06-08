package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.SysConfig;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>自提超时退回调度器</h2>
 *
 * <p>每天凌晨 3 点扫描"已送达自提点"状态超过 N 天的订单，
 * 自动取消退回仓库.</p>
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
    private final SysConfigMapper sysConfigMapper;

    /** 每天凌晨 3:00 执行 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cancelTimeoutPickupOrders() {
        log.info("开始扫描自提超时订单...");

        int timeoutDays = readTimeoutDays();

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(timeoutDays);

        // 查所有"已送达自提点"且超过 N 天未取的订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.RECEIVED.getCode())
                        .le(Order::getUpdateTime, cutoffTime)
        );

        if (orders.isEmpty()) {
            log.info("无自提超时订单需要处理");
            return;
        }

        int count = 0;
        for (Order order : orders) {
            order.setStatus(OrderStatus.CANCELLED.getCode());
            order.setCancelReason("自提超时" + timeoutDays + "天未取，退回仓库");
            orderMapper.updateById(order);

            log.info("自提超时退回: orderId={} orderNo={} updateTime={}",
                    order.getId(), order.getOrderNo(), order.getUpdateTime());
            count++;
        }

        log.info("自提超时扫描完成，共退回 {} 个订单", count);
    }

    /** 从 sys_config 读取超时天数配置，默认 3 天 */
    private int readTimeoutDays() {
        List<SysConfig> configs = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getConfigKey, "pickup_timeout_days")
        );
        if (configs.isEmpty()) {
            return 3;
        }
        try {
            return Integer.parseInt(configs.get(0).getConfigValue().trim());
        } catch (NumberFormatException e) {
            log.warn("pickup_timeout_days 配置解析失败，使用默认值 3 天", e);
            return 3;
        }
    }
}
