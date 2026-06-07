package com.duyell.communityfreshdelivery.service.impl;

import com.duyell.communityfreshdelivery.dto.OrderTimeoutMessage;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.OrderItem;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.OrderItemMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.ProductSkuMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * <h2>订单超时取消消费者</h2>
 *
 * <p>监听 {@code order.auto.cancel} 队列，收到延迟消息后检查订单状态：
 * 仍为待付款(0)则自动取消 + 回补库存，已支付则忽略.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductSkuMapper productSkuMapper;

    @RabbitListener(queues = "${app.biz.auto-cancel-queue}")
    @Transactional(rollbackFor = Exception.class)
    public void handleTimeout(OrderTimeoutMessage message, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到超时取消消息: orderId={}", message.getOrderId());

        Order order = orderMapper.selectById(message.getOrderId());
        if (order == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        /*
         * 幂等处理：只有待付款状态的订单才自动取消。
         * 已支付/已取消/后续状态 → 直接 ack 忽略。
         */
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT.getCode()) {
            log.info("订单状态已变更，忽略超时取消: orderNo={} status={}",
                    order.getOrderNo(), order.getStatus());
            channel.basicAck(deliveryTag, false);
            return;
        }

        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelReason("超时未支付，系统自动取消");
        orderMapper.updateById(order);

        // 回补库存
        List<OrderItem> items = orderItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
        );
        for (OrderItem item : items) {
            productSkuMapper.addStock(item.getSkuId(), item.getQuantity());
        }

        channel.basicAck(deliveryTag, false);
        log.info("订单超时自动取消完成: orderNo={} items={}", order.getOrderNo(), items.size());
    }
}
