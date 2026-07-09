package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>站内消息响应</h2>
 *
 * <p>含消息内容 + 类型文本，供前端展示.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class NotificationVO {

    /** 消息ID */
    private Long id;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息类型：1=系统通知 2=订单通知 3=退款通知 4=到货提醒 */
    private Integer type;

    /** 消息类型文本 */
    private String typeText;

    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;

    /** 发送时间 */
    private LocalDateTime createTime;
}
