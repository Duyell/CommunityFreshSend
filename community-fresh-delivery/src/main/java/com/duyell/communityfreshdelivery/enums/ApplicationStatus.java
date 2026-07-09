package com.duyell.communityfreshdelivery.enums;

import lombok.Getter;

/**
 * <h2>申请状态枚举</h2>
 *
 * <p>适用于团长申请 {@code group_leader_application} 和
 * 配送员申请 {@code delivery_application}.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Getter
public enum ApplicationStatus {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String text;

    ApplicationStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String textOf(int code) {
        for (ApplicationStatus s : values()) {
            if (s.code == code) {
                return s.text;
            }
        }
        return "未知";
    }
}
