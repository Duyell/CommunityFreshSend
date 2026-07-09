package com.duyell.communityfreshdelivery.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>RabbitMQ 配置</h2>
 *
 * <h3>延迟消息方案：死信队列（DLX）+ TTL</h3>
 * <p>无需安装额外插件，利用 RabbitMQ 内置的死信机制实现延迟投递：</p>
 * <pre>{@code
 * 发送消息 → order.delay.queue（TTL 15 分钟）
 *            │ 超时未消费 → 变成死信 → 投递到 order.cancel.exchange
 *            └────────────────────────→ order.auto.cancel 队列 → Consumer
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.mq.auto-cancel-queue}")
    private String cancelQueueName;

    @Value("${app.mq.order-timeout}")
    private int orderTimeoutMinutes;

    /** 取消交换机（死信消息投递到此处） */
    static final String CANCEL_EXCHANGE = "order.cancel.exchange";

    /** routingKey */
    static final String CANCEL_ROUTING_KEY = "order.auto.cancel";

    /** 延迟队列名 */
    private static final String DELAY_QUEUE = "order.delay.queue";

    /** 死信交换机（直连） */
    @Bean
    public DirectExchange cancelExchange() {
        return new DirectExchange(CANCEL_EXCHANGE, true, false);
    }

    /** 自动取消队列（Consumer 监听此队列） */
    @Bean
    public Queue autoCancelQueue() {
        return new Queue(cancelQueueName, true);
    }

    /** 绑定取消队列到取消交换机 */
    @Bean
    public Binding autoCancelBinding() {
        return BindingBuilder.bind(autoCancelQueue())
                .to(cancelExchange())
                .with(CANCEL_ROUTING_KEY);
    }

    /**
     * 延迟队列。
     * 消息在此队列中停留 TTL 时长后变为死信，自动投递到取消交换机。
     */
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .ttl(orderTimeoutMinutes * 60 * 1000)
                .deadLetterExchange(CANCEL_EXCHANGE)
                .deadLetterRoutingKey(CANCEL_ROUTING_KEY)
                .build();
    }

    /** JSON 消息序列化（替代默认的 Java 序列化） */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** RabbitTemplate 使用 JSON 序列化 */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
