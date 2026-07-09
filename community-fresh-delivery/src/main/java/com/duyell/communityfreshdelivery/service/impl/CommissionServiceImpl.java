package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.CommissionDetailVO;
import com.duyell.communityfreshdelivery.dto.CommissionVO;
import com.duyell.communityfreshdelivery.entity.CommissionRecord;
import com.duyell.communityfreshdelivery.entity.Order;
import com.duyell.communityfreshdelivery.entity.PickupPoint;
import com.duyell.communityfreshdelivery.mapper.CommissionRecordMapper;
import com.duyell.communityfreshdelivery.mapper.OrderMapper;
import com.duyell.communityfreshdelivery.mapper.PickupPointMapper;
import com.duyell.communityfreshdelivery.service.CommissionService;
import com.duyell.communityfreshdelivery.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>团长佣金服务实现</h2>
 *
 * <h3>佣金规则</h3>
 * <ul>
 *   <li>佣金比例从 sys_config.commission_rate 读取，默认 0.05（5%）</li>
 *   <li>用户自提核销时自动创建佣金记录</li>
 *   <li>提现简化：一键标记所有"未提现"为"已提现"</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final CommissionRecordMapper commissionRecordMapper;
    private final OrderMapper orderMapper;
    private final PickupPointMapper pickupPointMapper;
    private final SysConfigService sysConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getPickupPointId() == null) {
            return;
        }

        PickupPoint point = pickupPointMapper.selectById(order.getPickupPointId());
        if (point == null || point.getOwnerType() == null || point.getOwnerType() != 2
                || point.getOwnerId() == null) {
            return;
        }

        // 避免重复创建
        boolean exists = commissionRecordMapper.exists(
                new LambdaQueryWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getOrderId, orderId)
        );
        if (exists) {
            return;
        }

        BigDecimal rate = readCommissionRate();
        BigDecimal amount = order.getActualAmount().multiply(rate)
                .setScale(2, RoundingMode.DOWN);

        CommissionRecord record = new CommissionRecord();
        record.setUserId(point.getOwnerId());
        record.setPickupPointId(order.getPickupPointId());
        record.setOrderId(orderId);
        record.setOrderAmount(order.getActualAmount());
        record.setRate(rate);
        record.setAmount(amount);
        record.setStatus(0);

        commissionRecordMapper.insert(record);
        log.info("佣金已生成: orderId={} userId={} amount={}", orderId, point.getOwnerId(), amount);
    }

    @Override
    public CommissionVO summary() {
        Long userId = SecurityUtil.currentUserId();

        List<CommissionRecord> all = commissionRecordMapper.selectList(
                new LambdaQueryWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getUserId, userId)
        );

        BigDecimal total = all.stream()
                .map(CommissionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = all.stream()
                .filter(r -> r.getStatus() == 0)
                .map(CommissionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal withdrawn = all.stream()
                .filter(r -> r.getStatus() == 1)
                .map(CommissionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CommissionVO.builder()
                .totalCommission(total)
                .pendingWithdraw(pending)
                .withdrawn(withdrawn)
                .build();
    }

    @Override
    public Page<CommissionDetailVO> page(int page, int size, Integer status) {
        Long userId = SecurityUtil.currentUserId();

        LambdaQueryWrapper<CommissionRecord> wrapper = new LambdaQueryWrapper<CommissionRecord>()
                .eq(CommissionRecord::getUserId, userId)
                .orderByDesc(CommissionRecord::getCreateTime);
        if (status != null) {
            wrapper.eq(CommissionRecord::getStatus, status);
        }

        Page<CommissionRecord> result = commissionRecordMapper.selectPage(new Page<>(page, size), wrapper);

        List<CommissionDetailVO> vos = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        Page<CommissionDetailVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw() {
        Long userId = SecurityUtil.currentUserId();

        int updated = commissionRecordMapper.update(
                new LambdaUpdateWrapper<CommissionRecord>()
                        .eq(CommissionRecord::getUserId, userId)
                        .eq(CommissionRecord::getStatus, 0)
                        .set(CommissionRecord::getStatus, 1)
                        .set(CommissionRecord::getWithdrawTime, LocalDateTime.now())
        );

        if (updated == 0) {
            throw new BusinessException(100001, "没有可提现的佣金");
        }

        log.info("团长提现: userId={} count={}", userId, updated);
    }

    // ==================== 内部方法 ====================

    private BigDecimal readCommissionRate() {
        return sysConfigService.getDecimal("commission_rate", "0.05");
    }

    private CommissionDetailVO toVO(CommissionRecord entity) {
        String statusText = entity.getStatus() == 0 ? "未提现" : "已提现";

        return CommissionDetailVO.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .orderAmount(entity.getOrderAmount())
                .rate(entity.getRate())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .statusText(statusText)
                .withdrawTime(entity.getWithdrawTime())
                .createTime(entity.getCreateTime())
                .build();
    }
}
