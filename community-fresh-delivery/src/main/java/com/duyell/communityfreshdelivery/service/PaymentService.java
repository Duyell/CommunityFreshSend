package com.duyell.communityfreshdelivery.service;

/**
 * <h2>支付服务</h2>
 *
 * <p>策略模式：当前为模拟支付实现，后续可替换为微信/支付宝等真实支付渠道，
 * 接口签名不变.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
public interface PaymentService {

    /**
     * 执行支付.
     *
     * @param orderId 订单ID
     */
    void pay(Long orderId);

    /**
     * 取消支付（订单回到已取消状态 + 回补库存）.
     *
     * @param orderId 订单ID
     */
    void cancelPay(Long orderId);
}
