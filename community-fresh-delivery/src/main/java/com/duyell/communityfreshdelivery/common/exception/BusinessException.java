package com.duyell.communityfreshdelivery.common.exception;

import lombok.Getter;

/**
 * <h2>业务异常</h2>
 *
 * <p>Service 层遇到业务规则不满足时 <b>主动抛出此异常</b>，
 * 由 {@link GlobalExceptionHandler} 统一拦截并包装为 {@code Result} 返回前端.</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 默认 500
 * throw new BusinessException("用户不存在");
 *
 * // 自定义业务错误码
 * throw new BusinessException(10001, "商品库存不足");
 * throw new BusinessException(10002, "订单已超时，无法取消");
 * }</pre>
 *
 * <h3>错误码规划</h3>
 * <ul>
 *   <li>10001-19999 — 商品 / 库存</li>
 *   <li>20001-29999 — 订单 / 支付</li>
 *   <li>30001-39999 — 用户 / 认证</li>
 *   <li>40001-49999 — 配送 / 自提</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-05-31
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码，默认 500 */
    private final Integer code;

    /**
     * @param message 面向用户的错误提示
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * @param code    业务错误码（见类级文档）
     * @param message 面向用户的错误提示
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
