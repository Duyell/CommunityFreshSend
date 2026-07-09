package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.Payment;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>支付对账定时任务</h2>
 *
 * <p>每天凌晨 4:00 执行，对比昨日 payment（已支付）与 order（已支付状态），
 * 发现差异记录到日志.</p>
 *
 * <h3>检查项</h3>
 * <ul>
 *   <li>有支付记录但 order 不存在或状态异常</li>
 *   <li>有订单已支付但无对应 payment 记录</li>
 *   <li>支付金额与订单实付金额不一致</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    @Scheduled(cron = "0 0 4 * * ?")
    public void reconcile() {
        log.info("========== 开始支付对账 ==========");

        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().atStartOfDay();

        // 1. 查昨日支付记录
        List<Payment> payments = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .ge(Payment::getPaidTime, yesterdayStart)
                        .lt(Payment::getPaidTime, yesterdayEnd)
                        .eq(Payment::getStatus, 1)
        );

        if (payments.isEmpty()) {
            log.info("昨日无支付记录，对账结束");
            return;
        }

        // 2. 批量关联订单
        Set<Long> orderIds = payments.stream()
                .map(Payment::getOrderId)
                .collect(Collectors.toSet());
        Map<Long, Order> orderMap = orderMapper.selectList(
                        new LambdaQueryWrapper<Order>().in(Order::getId, orderIds)
                ).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        int matchCount = 0;
        int diffCount = 0;

        for (Payment payment : payments) {
            Order order = orderMap.get(payment.getOrderId());

            // 有支付无订单
            if (order == null) {
                log.error("对账差异-订单不存在: payNo={} orderId={} amount={}",
                        payment.getPayNo(), payment.getOrderId(), payment.getAmount());
                diffCount++;
                continue;
            }

            // 订单状态异常（未支付）
            if (order.getStatus() < 1) {
                log.error("对账差异-订单状态异常: payNo={} orderId={} orderStatus={} orderNo={}",
                        payment.getPayNo(), order.getId(), order.getStatus(), order.getOrderNo());
                diffCount++;
                continue;
            }

            // 金额不一致
            if (payment.getAmount().compareTo(order.getActualAmount()) != 0) {
                log.error("对账差异-金额不一致: payNo={} orderId={} orderNo={} payAmount={} orderAmount={}",
                        payment.getPayNo(), order.getId(), order.getOrderNo(),
                        payment.getAmount(), order.getActualAmount());
                diffCount++;
                continue;
            }

            matchCount++;
        }

        log.info("========== 对账完成: 匹配 {} 笔, 差异 {} 笔 ==========", matchCount, diffCount);
    }
}
