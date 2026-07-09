package com.duyell.communityfreshdelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <h2>收藏夹响应</h2>
 *
 * <p>含商品名称/最低价/首图，供前端展示.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Data
@Builder
public class FavoriteVO {

    /** 收藏ID */
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品最低价 */
    private BigDecimal minPrice;

    /** 商品首图 */
    private String productImage;

    /** 商品状态：1=上架 2=下架 3=售罄 */
    private Integer productStatus;

    /** 收藏时间 */
    private LocalDateTime createTime;
}
