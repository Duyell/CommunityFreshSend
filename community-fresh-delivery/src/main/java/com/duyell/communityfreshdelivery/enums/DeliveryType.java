package com.duyell.communityfreshdelivery.enums;

import lombok.Getter;

/**
 * <h2>配送方式枚举</h2>
 *
 * <p>对应 {@code order.delivery_type} 字段.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Getter
public enum DeliveryType {

    HOME_DELIVERY(1, "配送到家"),
    PICKUP(2, "送达自提点");

    private final int code;
    private final String text;

    DeliveryType(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static DeliveryType fromCode(int code) {
        for (DeliveryType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }

    public static String textOf(int code) {
        DeliveryType t = fromCode(code);
        return t != null ? t.text : "未知";
    }
}
