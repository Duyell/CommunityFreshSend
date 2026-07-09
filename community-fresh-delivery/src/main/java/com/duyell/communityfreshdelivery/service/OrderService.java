package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.OrderCreateDTO;
import com.duyell.communityfreshdelivery.dto.OrderVO;
import com.duyell.communityfreshdelivery.dto.SortItemDTO;

import java.util.List;

/**
 * <h2>订单服务</h2>
 *
 * <p>下单时原子完成：库存扣减 + 订单创建 + 购物车清空 + 延迟消息.
 * 用户只能查看/取消自己的订单.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
public interface OrderService {

    /**
     * 提交订单（核心事务）.
     *
     * @param dto 下单请求
     * @return 创建后的订单
     */
    OrderVO place(OrderCreateDTO dto);

    /**
     * 订单详情.
     *
     * @param orderId 订单ID
     * @return 订单（含明细）
     */
    OrderVO getById(Long orderId);

    /**
     * 我的订单分页.
     *
     * @param page   页码
     * @param size   每页条数
     * @param status 状态筛选，null=全部
     * @return 分页结果
     */
    Page<OrderVO> page(int page, int size, Integer status);

    /**
     * 用户取消订单（仅待付款/待接单/待分拣状态可取消）.
     *
     * @param orderId 订单ID
     * @param reason  取消原因
     */
    void cancel(Long orderId, String reason);

    /**
     * 商家接单（status 1→2）.
     * Controller 层 {@code @PreAuthorize("hasRole('MERCHANT')")} 控制权限.
     *
     * @param orderId 订单ID
     */
    void accept(Long orderId);

    /**
     * 商家分拣完成（status 2→3），订单进入待配送.
     * 逐项确认称重实重和缺货情况，计算称重差异金额（多退少补）.
     * Controller 层 {@code @PreAuthorize("hasRole('MERCHANT')")} 控制权限.
     *
     * @param orderId 订单ID
     * @param items   分拣明细（含实重/缺货标记）
     */
    void sortComplete(Long orderId, List<SortItemDTO> items);
}
