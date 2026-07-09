package com.duyell.communityfreshdelivery.service.impl;

import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.Payment;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.PaymentMapper;
import com.duyell.communityfreshdelivery.service.OperationLogService;
import com.duyell.communityfreshdelivery.service.OrderService;
import com.duyell.communityfreshdelivery.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * <h2>模拟支付实现</h2>
 *
 * <p>点击"已支付"后生成支付流水并更新订单状态，不对接真实支付网关.
 * 取消支付委托给 {@link OrderService#cancel(Long, String)} 复用库存回补逻辑.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Slf4j
@Service("mockPaymentService")
@RequiredArgsConstructor
public class MockPaymentServiceImpl implements PaymentService {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final OrderService orderService;
    private final OperationLogService operationLogService;

    private static final char[] PAYNO_CHARS = "ABCDEFGHJKMNPQRSTUVWXY3456789".toCharArray();
    private static final int PAYNO_RANDOM_LEN = 10;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        Long userId = SecurityUtil.currentUserId();
        Order order = orderMapper.selectById(orderId);

        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(20006, "订单不存在");
        }

        // 原子更新订单状态（防并发重复支付）
        int updated = orderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                        .set(Order::getStatus, OrderStatus.PENDING_ACCEPT.getCode())
                        .set(Order::getPaidTime, LocalDateTime.now())
        );
        if (updated == 0) {
            throw new BusinessException(20008, "当前订单状态不可支付");
        }

        // 生成支付流水号
        String payNo = generatePayNo();

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setPayNo(payNo);
        payment.setAmount(order.getActualAmount());
        // 模拟支付
        payment.setMethod(1);
        // 支付成功
        payment.setStatus(1);
        payment.setPaidTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        log.info("模拟支付成功: userId={} orderNo={} payNo={} amount={}",
                userId, order.getOrderNo(), payNo, order.getActualAmount());

        operationLogService.record(userId,
                OperationLogService.ORDER_PAY,
                OperationLogService.TARGET_ORDER,
                orderId,
                OrderStatus.PENDING_PAYMENT.getText(),
                OrderStatus.PENDING_ACCEPT.getText(),
                "支付成功，流水号:" + payNo);
    }

    @Override
    public void cancelPay(Long orderId) {
        // 复用 OrderService.cancel()，不再重复写库存回补逻辑
        orderService.cancel(orderId, "用户取消支付");
        log.info("取消支付: userId={} orderId={}", SecurityUtil.currentUserId(), orderId);
    }

    /** 生成支付流水号：PAY + 时间戳 + 随机码 */
    private String generatePayNo() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        StringBuilder sb = new StringBuilder("PAY");
        sb.append(timestamp);
        for (int i = 0; i < PAYNO_RANDOM_LEN; i++) {
            sb.append(PAYNO_CHARS[random.nextInt(PAYNO_CHARS.length)]);
        }
        return sb.toString();
    }
}
