package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>配送记录响应</h2>
 *
 * <p>接单大厅列表和我的配送记录共用，拼装订单摘要信息便于展示.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@Data
@Builder
public class DeliveryVO {

    /** 配送记录ID */
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 配送员用户ID */
    private Long deliveryUserId;

    /** 配送状态码 */
    private Integer status;

    /** 配送状态文本 */
    private String statusText;

    /** 配送方式文本（配送到家 / 送达自提点） */
    private String deliveryTypeText;

    /** 送达地址描述 */
    private String address;

    /** 配送时段 */
    private String deliveryTimeSlot;

    /** 取货确认时间 */
    private LocalDateTime pickupTime;

    /** 送达确认时间 */
    private LocalDateTime deliverTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
