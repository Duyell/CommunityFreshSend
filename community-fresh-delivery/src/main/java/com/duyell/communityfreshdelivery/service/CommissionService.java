package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.CommissionDetailVO;
import com.duyell.communityfreshdelivery.dto.CommissionVO;

/**
 * <h2>团长佣金服务</h2>
 *
 * <p>自提核销自动分成 + 收益查询 + 提现.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
public interface CommissionService {

    /**
     * 创建佣金记录（核销时自动调用）.
     *
     * @param orderId 关联订单ID
     */
    void create(Long orderId);

    /**
     * 团长收益概览.
     *
     * @return 累计/待提现/已提现
     */
    CommissionVO summary();

    /**
     * 佣金明细分页.
     *
     * @param page   页码
     * @param size   每页条数
     * @param status 状态筛选（null=全部）
     * @return 分页结果
     */
    Page<CommissionDetailVO> page(int page, int size, Integer status);

    /**
     * 一键提现（将未提现佣金全部标记为已提现）.
     */
    void withdraw();
}
