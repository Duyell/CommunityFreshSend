package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.DeliveryApplyDTO;
import com.duyell.communityfreshdelivery.dto.DeliveryApplicationVO;
import com.duyell.communityfreshdelivery.dto.DeliveryReviewDTO;
import com.duyell.communityfreshdelivery.entity.DeliveryApplication;
import com.duyell.communityfreshdelivery.entity.UserRole;
import com.duyell.communityfreshdelivery.mapper.DeliveryApplicationMapper;
import com.duyell.communityfreshdelivery.mapper.UserRoleMapper;
import com.duyell.communityfreshdelivery.enums.ApplicationStatus;
import com.duyell.communityfreshdelivery.service.DeliveryApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>配送员申请服务实现</h2>
 *
 * <h3>审核流程</h3>
 * <pre>{@code
 * 用户提交 apply() → status=PENDING（待审核）
 *   │
 *   ├── 管理员 approve:
 *   │     1. physicalDelete + INSERT user_role（ROLE_DELIVERY）
 *   │     2. UPDATE application.status=APPROVED
 *   │
 *   └── 管理员 reject:
 *         UPDATE application.status=REJECTED, reject_reason
 * }</pre>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryApplicationServiceImpl implements DeliveryApplicationService {

    private final DeliveryApplicationMapper applicationMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public DeliveryApplicationVO apply(DeliveryApplyDTO dto) {
        Long userId = SecurityUtil.currentUserId();

        // 校验是否已有待审核申请
        boolean hasPending = applicationMapper.exists(
                new LambdaQueryWrapper<DeliveryApplication>()
                        .eq(DeliveryApplication::getUserId, userId)
                        .eq(DeliveryApplication::getStatus, ApplicationStatus.PENDING.getCode())
        );
        if (hasPending) {
            throw new BusinessException(80001, "您已有待审核的申请，请耐心等待");
        }

        DeliveryApplication entity = new DeliveryApplication();
        entity.setUserId(userId);
        entity.setRealName(dto.getRealName());
        entity.setPhone(dto.getPhone());
        entity.setRemark(dto.getRemark() != null ? dto.getRemark() : "");
        entity.setStatus(ApplicationStatus.PENDING.getCode());

        applicationMapper.insert(entity);
        log.info("配送员申请已提交: userId={} id={}", userId, entity.getId());

        return toVO(entity);
    }

    @Override
    public DeliveryApplicationVO myApplication() {
        Long userId = SecurityUtil.currentUserId();

        List<DeliveryApplication> list = applicationMapper.selectList(
                new LambdaQueryWrapper<DeliveryApplication>()
                        .eq(DeliveryApplication::getUserId, userId)
                        .orderByDesc(DeliveryApplication::getCreateTime)
                        .last("LIMIT 1")
        );

        if (list.isEmpty()) {
            return DeliveryApplicationVO.builder()
                    .statusText("未申请")
                    .build();
        }

        return toVO(list.get(0));
    }

    @Override
    public Page<DeliveryApplicationVO> pageApplications(int page, int size, Integer status) {
        LambdaQueryWrapper<DeliveryApplication> wrapper =
                new LambdaQueryWrapper<DeliveryApplication>();
        if (status != null) {
            wrapper.eq(DeliveryApplication::getStatus, status);
        }
        wrapper.orderByAsc(DeliveryApplication::getCreateTime);

        Page<DeliveryApplication> result = applicationMapper.selectPage(
                new Page<>(page, size), wrapper
        );

        List<DeliveryApplicationVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        Page<DeliveryApplicationVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeliveryApplicationVO review(Long id, DeliveryReviewDTO dto) {
        DeliveryApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException(80002, "申请记录不存在");
        }
        if (application.getStatus() != ApplicationStatus.PENDING.getCode()) {
            throw new BusinessException(80003, "该申请已审核，请勿重复操作");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // 通过：追加 ROLE_DELIVERY
            // 先物理清除旧记录（含软删除的），释放唯一键占位，再插入
            userRoleMapper.physicalDelete(application.getUserId(), "ROLE_DELIVERY");
            UserRole userRole = new UserRole();
            userRole.setUserId(application.getUserId());
            userRole.setRole("ROLE_DELIVERY");
            userRoleMapper.insert(userRole);

            application.setStatus(ApplicationStatus.APPROVED.getCode());
            application.setReviewedTime(LocalDateTime.now());
            applicationMapper.updateById(application);

            log.info("配送员申请已通过: id={} userId={}", id, application.getUserId());
        } else {
            // 拒绝
            if (dto.getRejectReason() == null || dto.getRejectReason().isBlank()) {
                throw new BusinessException(80004, "拒绝原因不能为空");
            }
            application.setStatus(ApplicationStatus.REJECTED.getCode());
            application.setRejectReason(dto.getRejectReason());
            application.setReviewedTime(LocalDateTime.now());
            applicationMapper.updateById(application);

            log.info("配送员申请已拒绝: id={} userId={} reason={}",
                    id, application.getUserId(), dto.getRejectReason());
        }

        return toVO(application);
    }

    // ==================== 内部方法 ====================

    private DeliveryApplicationVO toVO(DeliveryApplication entity) {
        String statusText = ApplicationStatus.textOf(entity.getStatus());

        return DeliveryApplicationVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .realName(entity.getRealName())
                .phone(entity.getPhone())
                .remark(entity.getRemark())
                .status(entity.getStatus())
                .statusText(statusText)
                .rejectReason(entity.getRejectReason())
                .reviewedTime(entity.getReviewedTime())
                .createTime(entity.getCreateTime())
                .build();
    }
}
