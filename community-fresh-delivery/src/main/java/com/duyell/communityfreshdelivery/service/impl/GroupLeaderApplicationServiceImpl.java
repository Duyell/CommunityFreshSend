package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplyDTO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplicationVO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderReviewDTO;
import com.duyell.communityfreshdelivery.entity.GroupLeaderApplication;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.PickupPoint;
import com.duyell.communityfreshdelivery.entity.UserRole;
import com.duyell.communityfreshdelivery.enums.OrderStatus;
import com.duyell.communityfreshdelivery.mapper.GroupLeaderApplicationMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.PickupPointMapper;
import com.duyell.communityfreshdelivery.mapper.UserRoleMapper;
import com.duyell.communityfreshdelivery.service.GroupLeaderApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * <h2>团长申请与核销服务实现</h2>
 *
 * <h3>申请审核流程</h3>
 * <pre>{@code
 * 用户提交 apply() → status=0（待审核）
 *   │
 *   ├── 管理员 approve:
 *   │     1. INSERT pickup_point（owner_type=2, owner_id=userId）
 *   │     2. INSERT user_role（ROLE_GROUP_LEADER）
 *   │     3. UPDATE application.status=1
 *   │
 *   └── 管理员 reject:
 *         UPDATE application.status=2, reject_reason
 * }</pre>
 *
 * <h3>提货码核销</h3>
 * <pre>{@code
 * 团长输入提货码 → 校验所属自提点 → 校验订单状态=5（已送达自提点）
 *   → 更新 order.status=6（用户已自提）+ pickupTime
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupLeaderApplicationServiceImpl implements GroupLeaderApplicationService {

    private final GroupLeaderApplicationMapper applicationMapper;
    private final PickupPointMapper pickupPointMapper;
    private final UserRoleMapper userRoleMapper;
    private final OrderMapper orderMapper;

    // ==================== 申请 ====================

    @Override
    public GroupLeaderApplicationVO apply(GroupLeaderApplyDTO dto) {
        Long userId = SecurityUtil.currentUserId();

        // 校验是否已有待审核的申请
        boolean hasPending = applicationMapper.exists(
                new LambdaQueryWrapper<GroupLeaderApplication>()
                        .eq(GroupLeaderApplication::getUserId, userId)
                        .eq(GroupLeaderApplication::getStatus, 0)
        );
        if (hasPending) {
            throw new BusinessException(40001, "您已有待审核的申请，请耐心等待");
        }

        GroupLeaderApplication entity = new GroupLeaderApplication();
        entity.setUserId(userId);
        entity.setAddress(dto.getAddress());
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setRemark(dto.getRemark() != null ? dto.getRemark() : "");
        entity.setStatus(0);

        applicationMapper.insert(entity);
        log.info("团长申请已提交: userId={} id={}", userId, entity.getId());

        return toVO(entity);
    }

    @Override
    public GroupLeaderApplicationVO myApplication() {
        Long userId = SecurityUtil.currentUserId();

        List<GroupLeaderApplication> list = applicationMapper.selectList(
                new LambdaQueryWrapper<GroupLeaderApplication>()
                        .eq(GroupLeaderApplication::getUserId, userId)
                        .orderByDesc(GroupLeaderApplication::getCreateTime)
                        .last("LIMIT 1")
        );

        if (list.isEmpty()) {
            return GroupLeaderApplicationVO.builder()
                    .statusText("未申请")
                    .build();
        }

        return toVO(list.get(0));
    }

    // ==================== 管理员审核 ====================

    @Override
    public Page<GroupLeaderApplicationVO> pageApplications(int page, int size, Integer status) {
        LambdaQueryWrapper<GroupLeaderApplication> wrapper =
                new LambdaQueryWrapper<GroupLeaderApplication>();
        if (status != null) {
            wrapper.eq(GroupLeaderApplication::getStatus, status);
        }
        wrapper.orderByAsc(GroupLeaderApplication::getCreateTime);

        Page<GroupLeaderApplication> result = applicationMapper.selectPage(
                new Page<>(page, size), wrapper
        );

        List<GroupLeaderApplicationVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        Page<GroupLeaderApplicationVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupLeaderApplicationVO review(Long id, GroupLeaderReviewDTO dto) {
        GroupLeaderApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(40002, "申请记录不存在");
        }
        if (application.getStatus() != 0) {
            throw new BusinessException(40003, "该申请已审核，请勿重复操作");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // -------- 通过 --------
            // 1. 创建团长自提点
            PickupPoint pickupPoint = new PickupPoint();
            pickupPoint.setName(application.getAddress().length() > 100
                    ? application.getAddress().substring(0, 100)
                    : application.getAddress());
            pickupPoint.setContact(application.getContactName());
            pickupPoint.setPhone(application.getContactPhone());
            pickupPoint.setAddress(application.getAddress());
            pickupPoint.setOwnerType(2);
            pickupPoint.setOwnerId(application.getUserId());
            pickupPoint.setStatus(1);
            pickupPointMapper.insert(pickupPoint);

            // 2. 追加 ROLE_GROUP_LEADER 角色（先物理清除软删除记录再插入，防唯一约束冲突）
            // 先物理清除软删除的记录（@TableLogic 软删除后唯一约束仍占位）
            userRoleMapper.physicalDelete(application.getUserId(), "ROLE_GROUP_LEADER");

            boolean hasRole = userRoleMapper.exists(
                    new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, application.getUserId())
                            .eq(UserRole::getRole, "ROLE_GROUP_LEADER")
            );
            if (!hasRole) {
                UserRole userRole = new UserRole();
                userRole.setUserId(application.getUserId());
                userRole.setRole("ROLE_GROUP_LEADER");
                userRoleMapper.insert(userRole);
            }

            // 3. 更新申请状态
            application.setStatus(1);
            application.setReviewedTime(LocalDateTime.now());
            applicationMapper.updateById(application);

            log.info("团长申请已通过: id={} userId={} pickupPointId={}",
                    id, application.getUserId(), pickupPoint.getId());
        } else {
            // -------- 拒绝 --------
            if (dto.getRejectReason() == null || dto.getRejectReason().isBlank()) {
                throw new BusinessException(40004, "拒绝原因不能为空");
            }
            application.setStatus(2);
            application.setRejectReason(dto.getRejectReason());
            application.setReviewedTime(LocalDateTime.now());
            applicationMapper.updateById(application);

            log.info("团长申请已拒绝: id={} userId={} reason={}",
                    id, application.getUserId(), dto.getRejectReason());
        }

        return toVO(application);
    }

    // ==================== 提货核销 ====================

    @Override
    public void verifyPickup(String pickupCode) {
        Long userId = SecurityUtil.currentUserId();

        // 1. 查团长所属自提点
        List<PickupPoint> points = pickupPointMapper.selectList(
                new LambdaQueryWrapper<PickupPoint>()
                        .eq(PickupPoint::getOwnerType, 2)
                        .eq(PickupPoint::getOwnerId, userId)
                        .eq(PickupPoint::getStatus, 1)
        );
        if (points.isEmpty()) {
            throw new BusinessException(40005, "您不是团长，无权核销");
        }
        Long myPickupPointId = points.get(0).getId();

        // 2. 按提货码查订单
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPickupCode, pickupCode)
        );
        if (orders.isEmpty()) {
            throw new BusinessException(40006, "提货码无效");
        }
        Order order = orders.get(0);

        // 3. 校验订单归属自提点
        if (order.getPickupPointId() == null
                || !order.getPickupPointId().equals(myPickupPointId)) {
            throw new BusinessException(40008, "该订单不属于您的自提点");
        }

        // 4. 校验订单状态
        if (order.getStatus() != OrderStatus.RECEIVED.getCode()) {
            throw new BusinessException(40007, "该订单尚未到达自提点，无法核销");
        }

        // 5. 更新订单状态 → 用户已自提
        order.setStatus(OrderStatus.PICKED_UP.getCode());
        order.setPickupTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("自提核销完成: pickupCode={} orderNo={} pickupPointId={}",
                pickupCode, order.getOrderNo(), myPickupPointId);
    }

    // ==================== 团长自提点 ====================

    @Override
    public List<PickupPoint> myPickupPoint() {
        Long userId = SecurityUtil.currentUserId();

        return pickupPointMapper.selectList(
                new LambdaQueryWrapper<PickupPoint>()
                        .eq(PickupPoint::getOwnerType, 2)
                        .eq(PickupPoint::getOwnerId, userId)
                        .eq(PickupPoint::getStatus, 1)
        );
    }

    // ==================== 内部方法 ====================

    /** Entity → VO */
    private GroupLeaderApplicationVO toVO(GroupLeaderApplication entity) {
        String statusText = switch (entity.getStatus()) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            default -> "未知";
        };

        return GroupLeaderApplicationVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .address(entity.getAddress())
                .contactName(entity.getContactName())
                .contactPhone(entity.getContactPhone())
                .remark(entity.getRemark())
                .status(entity.getStatus())
                .statusText(statusText)
                .rejectReason(entity.getRejectReason())
                .reviewedTime(entity.getReviewedTime())
                .createTime(entity.getCreateTime())
                .build();
    }
}
