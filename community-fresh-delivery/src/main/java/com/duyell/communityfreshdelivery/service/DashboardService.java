package com.duyell.communityfreshdelivery.service;

import com.duyell.communityfreshdelivery.dto.DashboardVO;
import com.duyell.communityfreshdelivery.dto.HotProductVO;
import com.duyell.communityfreshdelivery.dto.SalesStatsVO;

import java.util.List;

/**
 * <h2>数据看板服务</h2>
 *
 * <p>商家端首页数据统计：今日概况 / 销量统计 / 热销排行.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
public interface DashboardService {

    /**
     * 今日概况.
     *
     * @return 今日各状态订单数 + 销售额
     */
    DashboardVO today();

    /**
     * 销量统计（按日/周/月）.
     *
     * @param period day / week / month
     * @return 各时段销量 + 销售额 + 订单数
     */
    List<SalesStatsVO> salesStats(String period);

    /**
     * 热销 TOP 10.
     *
     * @return 销量前10的商品
     */
    List<HotProductVO> hotProducts();
}
