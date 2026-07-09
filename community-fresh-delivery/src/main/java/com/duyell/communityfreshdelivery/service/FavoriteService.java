package com.duyell.communityfreshdelivery.service;

import com.duyell.communityfreshdelivery.dto.FavoriteVO;

import java.util.List;

/**
 * <h2>收藏夹服务</h2>
 *
 * <p>用户收藏/取消收藏商品，查看收藏列表.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
public interface FavoriteService {

    /**
     * 收藏商品.
     *
     * @param productId 商品ID
     */
    void add(Long productId);

    /**
     * 取消收藏.
     *
     * @param productId 商品ID
     */
    void remove(Long productId);

    /**
     * 我的收藏列表.
     *
     * @return 收藏列表（含商品信息）
     */
    List<FavoriteVO> list();

    /**
     * 检查是否已收藏.
     *
     * @param productId 商品ID
     * @return true=已收藏
     */
    boolean isFavorited(Long productId);
}
