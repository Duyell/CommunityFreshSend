package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>评价响应</h2>
 *
 * <p>含评价信息 + 用户昵称 + 商品名称，供前端展示.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Data
@Builder
public class ReviewVO {

    /** 评价ID */
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 评价用户ID */
    private Long userId;

    /** 用户昵称 */
    private String userNickname;

    /** 被评价商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 评分（1-5星） */
    private Integer score;

    /** 文字评价 */
    private String content;

    /** 评价图片 */
    private String images;

    /** 评价时间 */
    private LocalDateTime createTime;
}
