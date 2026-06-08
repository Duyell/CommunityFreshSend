package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.ReviewCreateDTO;
import com.duyell.communityfreshdelivery.dto.ReviewVO;

import java.util.List;

/**
 * <h2>评价服务</h2>
 *
 * <p>用户收货后对订单商品评分评价，评价后订单自动完成.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
public interface ReviewService {

    /**
     * 创建评价.
     *
     * @param dto 评价内容
     * @return 评价记录
     */
    ReviewVO create(ReviewCreateDTO dto);

    /**
     * 查看订单的所有评价.
     *
     * @param orderId 订单ID
     * @return 评价列表
     */
    List<ReviewVO> getByOrderId(Long orderId);

    /**
     * 查看商品的评价列表（公开）.
     *
     * @param productId 商品ID
     * @param page      页码
     * @param size      每页条数
     * @return 分页结果
     */
    Page<ReviewVO> getByProductId(Long productId, int page, int size);

    /**
     * 我的评价历史.
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    Page<ReviewVO> myReviews(int page, int size);
}
