package com.duyell.communityfreshdelivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplyDTO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplicationVO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderReviewDTO;
import com.duyell.communityfreshdelivery.entity.PickupPoint;

import java.util.List;

/**
 * <h2>团长申请与核销服务</h2>
 *
 * <p>覆盖团长申请全生命周期：提交申请 → 管理员审核 → 创建自提点 → 提货码核销.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
public interface GroupLeaderApplicationService {

    /**
     * 用户提交团长申请.
     *
     * @param dto 申请信息
     * @return 申请记录
     */
    GroupLeaderApplicationVO apply(GroupLeaderApplyDTO dto);

    /**
     * 查看我的最新申请状态.
     *
     * @return 申请记录（未申请时返回空 VO）
     */
    GroupLeaderApplicationVO myApplication();

    /**
     * 管理员分页查看申请列表.
     *
     * @param page   页码
     * @param size   每页条数
     * @param status 状态筛选，null=全部
     * @return 分页结果
     */
    Page<GroupLeaderApplicationVO> pageApplications(int page, int size, Integer status);

    /**
     * 管理员审核申请（通过/拒绝）.
     *
     * @param id  申请ID
     * @param dto 审核结果
     * @return 更新后的申请记录
     */
    GroupLeaderApplicationVO review(Long id, GroupLeaderReviewDTO dto);

    /**
     * 团长输入提货码核销自提订单.
     *
     * @param pickupCode 6位数字提货码
     */
    void verifyPickup(String pickupCode);

    /**
     * 团长查看自己的自提点.
     *
     * @return 自提点列表（通常一个团长只有一个）
     */
    List<PickupPoint> myPickupPoint();
}
