package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.DeliveryApplyDTO;
import com.duyell.communityfreshdelivery.dto.DeliveryApplicationVO;
import com.duyell.communityfreshdelivery.dto.DeliveryReviewDTO;

/**
 * <h2>配送员申请服务</h2>
 *
 * <p>用户提交申请 → 管理员审核 → 通过后追加 ROLE_DELIVERY.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
public interface DeliveryApplicationService {

    /**
     * 提交配送员申请.
     *
     * @param dto 申请信息
     * @return 申请记录
     */
    DeliveryApplicationVO apply(DeliveryApplyDTO dto);

    /**
     * 查询我的申请状态.
     *
     * @return 最新一条申请记录
     */
    DeliveryApplicationVO myApplication();

    /**
     * 管理员分页查申请列表.
     *
     * @param page   页码
     * @param size   每页条数
     * @param status 状态筛选（null=全部）
     * @return 分页结果
     */
    Page<DeliveryApplicationVO> pageApplications(int page, int size, Integer status);

    /**
     * 管理员审核申请.
     *
     * @param id  申请ID
     * @param dto 审核信息
     * @return 更新后的申请记录
     */
    DeliveryApplicationVO review(Long id, DeliveryReviewDTO dto);
}
