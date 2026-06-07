package com.duyell.communityfreshdelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h2>订单超时延迟消息体</h2>
 *
 * <p>下单后发送到 RabbitMQ 延迟交换机，15 分钟后投递到取消队列，
 * 消费者收到后检查订单是否仍未支付，是则自动取消.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMessage {

    /** 订单ID */
    private Long orderId;
}
