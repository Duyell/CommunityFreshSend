package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.CouponIssueDTO;
import com.duyell.communityfreshdelivery.dto.CouponSaveDTO;
import com.duyell.communityfreshdelivery.dto.CouponVO;
import com.duyell.communityfreshdelivery.dto.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * <h2>优惠券服务</h2>
 *
 * <p>管理员端：模板 CRUD + 发券；用户端：查券 + 下单选券.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
public interface CouponService {

    // ==================== 管理员端 ====================

    /**
     * 创建优惠券模板.
     *
     * @param dto 模板信息
     * @return 创建后的模板
     */
    CouponVO create(CouponSaveDTO dto);

    /**
     * 编辑优惠券模板.
     *
     * @param id  模板ID
     * @param dto 新信息
     * @return 更新后的模板
     */
    CouponVO update(Long id, CouponSaveDTO dto);

    /**
     * 启用/停用模板.
     *
     * @param id     模板ID
     * @param status 1=启用 0=停用
     */
    void updateStatus(Long id, Integer status);

    /**
     * 删除券模板.
     *
     * @param id 模板ID
     */
    void delete(Long id);

    /**
     * 分页查券模板.
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    Page<CouponVO> page(int page, int size);

    /**
     * 管理员给用户发券.
     *
     * @param dto 发放信息
     */
    void issue(CouponIssueDTO dto);

    // ==================== 用户端 ====================

    /**
     * 我的优惠券列表.
     *
     * @param status 状态筛选：null=全部 0=未使用 1=已使用 2=已过期
     * @return 券列表
     */
    List<UserCouponVO> myCoupons(Integer status);

    /**
     * 下单时获取可用优惠券列表.
     * 过滤规则：未使用 + 未过期 + 门槛满足 + 范围匹配（全场 or 包含订单中商品分类）.
     *
     * @param orderAmount 订单商品合计金额
     * @param categoryIds 订单中商品的分类ID集合
     * @return 可用券列表（含预估优惠金额）
     */
    List<UserCouponVO> listAvailable(BigDecimal orderAmount, List<Long> categoryIds);

    // ==================== 领券中心 ====================

    /**
     * 领券中心 — 列出所有可领取的券模板.
     * 排除用户已领过的模板，仅展示启用状态的券.
     *
     * @return 可领取的券模板列表
     */
    List<CouponVO> listCenter();

    /**
     * 用户领取优惠券.
     *
     * @param couponId 券模板ID
     */
    void claim(Long couponId);
}
