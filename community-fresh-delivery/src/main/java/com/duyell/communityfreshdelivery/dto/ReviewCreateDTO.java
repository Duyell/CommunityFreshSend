package com.duyell.communityfreshdelivery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * <h2>评价创建请求</h2>
 *
 * <p>用户收货后对订单中的商品进行评分和文字评价.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Data
public class ReviewCreateDTO {

    /** 订单ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 被评价商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 评分（1-5星） */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1星")
    @Max(value = 5, message = "评分最高为5星")
    private Integer score;

    /** 文字评价（可选） */
    private String content;

    /** 评价图片 OSS Key 列表（JSON 数组字符串，可选） */
    private String images;
}
