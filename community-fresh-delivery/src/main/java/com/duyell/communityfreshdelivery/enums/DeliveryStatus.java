package com.duyell.communityfreshdelivery.enums;

import lombok.Getter;

/**
 * <h2>配送状态枚举</h2>
 *
 * <p>配送记录（delivery 表）的 status 字段.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Getter
public enum DeliveryStatus {

    WAITING_PICKUP(1, "待取货"),
    IN_DELIVERY(2, "配送中"),
    DELIVERED(3, "已送达");

    private final int code;
    private final String text;

    DeliveryStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static DeliveryStatus fromCode(int code) {
        for (DeliveryStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String textOf(int code) {
        DeliveryStatus s = fromCode(code);
        return s != null ? s.text : "未知状态";
    }
}
