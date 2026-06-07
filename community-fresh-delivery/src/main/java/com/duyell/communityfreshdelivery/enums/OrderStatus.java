package com.duyell.communityfreshdelivery.enums;

import lombok.Getter;

/**
 * <h2>订单状态枚举</h2>
 *
 * <p>状态码与 {@code order.status} 字段一致，提供 {@code code ↔ text} 的互转.</p>
 *
 * <pre>{@code
 * 待付款(0) → 待接单(1) → 待分拣(2) → 待配送(3) → 配送中(4) → 已签收(5) → 待评价(7) → 已完成(8)
 *                                                                    ↘ 用户已自提(6) ↗
 * 待付款(0) → 已取消(9)
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Getter
public enum OrderStatus {

    PENDING_PAYMENT(0, "待付款"),
    PENDING_ACCEPT(1, "待接单"),
    PENDING_SORTING(2, "待分拣"),
    PENDING_DELIVERY(3, "待配送"),
    IN_DELIVERY(4, "配送中"),
    RECEIVED(5, "已签收/已送达自提点"),
    PICKED_UP(6, "用户已自提"),
    PENDING_REVIEW(7, "待评价"),
    COMPLETED(8, "已完成"),
    CANCELLED(9, "已取消"),
    REFUNDING(10, "退款中"),
    REFUNDED(11, "已退款");

    private final int code;
    private final String text;

    OrderStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    /** 根据状态码获取枚举 */
    public static OrderStatus fromCode(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    /** 根据状态码获取中文文本 */
    public static String textOf(int code) {
        OrderStatus s = fromCode(code);
        return s != null ? s.text : "未知状态";
    }
}
