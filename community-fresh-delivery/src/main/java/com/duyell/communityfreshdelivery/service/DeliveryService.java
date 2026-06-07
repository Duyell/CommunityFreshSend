package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.DeliveryVO;

/**
 * <h2>配送服务</h2>
 *
 * <p>配送员抢单/取货/送达，全部需 ROLE_DELIVERY 角色.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
public interface DeliveryService {

    /**
     * 接单大厅（所有待配送状态的订单）.
     *
     * @param page 页码
     * @param size 每页条数
     * @return 待配送订单列表
     */
    Page<DeliveryVO> hall(int page, int size);

    /**
     * 抢单.
     *
     * @param orderId 订单ID
     * @return 配送记录
     */
    DeliveryVO grab(Long orderId);

    /**
     * 取货确认（status 1→2）.
     *
     * @param orderId 订单ID
     */
    void confirmPickup(Long orderId);

    /**
     * 送达确认（status 2→3 + 订单状态 4→5）.
     *
     * @param orderId 订单ID
     */
    void confirmDelivery(Long orderId);

    /**
     * 我的配送记录.
     *
     * @param page   页码
     * @param size   每页条数
     * @param status 状态筛选，null=全部
     * @return 配送记录分页
     */
    Page<DeliveryVO> myDeliveries(int page, int size, Integer status);
}
