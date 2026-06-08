package com.duyell.communityfreshdelivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>评价实体</h2>
 *
 * <p>对应 {@code review} 表。用户收货后对商品进行评分和文字评价，
 * 一个订单的一个商品只能评价一次.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Data
@TableName("review")
public class Review {

    /** 评价ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 评价用户ID */
    private Long userId;

    /** 被评价商品ID */
    private Long productId;

    /** 评分（1-5星） */
    private Integer score;

    /** 文字评价 */
    private String content;

    /** 评价图片 OSS Key 列表（JSON 数组字符串） */
    private String images;

    /** 评价时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
