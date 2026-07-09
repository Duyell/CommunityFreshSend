package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.common.utils.SecurityUtil;
import com.duyell.communityfreshdelivery.dto.CouponIssueDTO;
import com.duyell.communityfreshdelivery.dto.CouponSaveDTO;
import com.duyell.communityfreshdelivery.dto.CouponVO;
import com.duyell.communityfreshdelivery.dto.UserCouponVO;
import com.duyell.communityfreshdelivery.entity.Coupon;
import com.duyell.communityfreshdelivery.entity.UserCoupon;
import com.duyell.communityfreshdelivery.mapper.CouponMapper;
import com.duyell.communityfreshdelivery.mapper.UserCouponMapper;
import com.duyell.communityfreshdelivery.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>优惠券服务实现</h2>
 *
 * <h3>优惠金额计算规则</h3>
 * <ul>
 *   <li>满减券/新人券/品类券：discountAmount = discountValue</li>
 *   <li>折扣券：discountAmount = orderAmount × (1 - discountValue)</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    // ==================== 管理员端 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponVO create(CouponSaveDTO dto) {
        validateCouponType(dto.getType(), dto.getDiscountValue());

        Coupon coupon = new Coupon();
        coupon.setName(dto.getName());
        coupon.setType(dto.getType());
        coupon.setThreshold(dto.getThreshold() != null ? dto.getThreshold() : BigDecimal.ZERO);
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setScopeType(dto.getScopeType() != null ? dto.getScopeType() : 0);
        coupon.setScopeId(dto.getScopeId());
        coupon.setValidDays(dto.getValidDays());
        coupon.setStatus(1);

        couponMapper.insert(coupon);
        log.info("优惠券模板创建成功: id={} name={} type={}", coupon.getId(), coupon.getName(), coupon.getType());

        return toCouponVO(coupon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponVO update(Long id, CouponSaveDTO dto) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(70001, "优惠券模板不存在");
        }

        validateCouponType(dto.getType(), dto.getDiscountValue());

        coupon.setName(dto.getName());
        coupon.setType(dto.getType());
        coupon.setThreshold(dto.getThreshold() != null ? dto.getThreshold() : BigDecimal.ZERO);
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setScopeType(dto.getScopeType() != null ? dto.getScopeType() : 0);
        coupon.setScopeId(dto.getScopeId());
        coupon.setValidDays(dto.getValidDays());

        couponMapper.updateById(coupon);
        log.info("优惠券模板更新成功: id={}", id);

        return toCouponVO(coupon);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(70001, "优惠券模板不存在");
        }
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
        log.info("优惠券状态变更: id={} status={}", id, status);
    }

    @Override
    public void delete(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException(70001, "优惠券模板不存在");
        }
        couponMapper.deleteById(id);
        log.info("优惠券模板已删除: id={}", id);
    }

    @Override
    public Page<CouponVO> page(int page, int size) {
        Page<Coupon> result = couponMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Coupon>()
                        .orderByDesc(Coupon::getCreateTime)
        );

        List<CouponVO> vos = result.getRecords().stream()
                .map(this::toCouponVO)
                .toList();

        Page<CouponVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issue(CouponIssueDTO dto) {
        Coupon coupon = couponMapper.selectById(dto.getCouponId());
        if (coupon == null) {
            throw new BusinessException(70001, "优惠券模板不存在");
        }
        if (coupon.getStatus() != 1) {
            throw new BusinessException(70002, "该优惠券已停用，无法发放");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(dto.getUserId());
        userCoupon.setCouponId(dto.getCouponId());
        userCoupon.setStatus(0);
        userCoupon.setExpireTime(LocalDateTime.now().plusDays(coupon.getValidDays()));

        userCouponMapper.insert(userCoupon);
        log.info("优惠券已发放: userId={} couponId={} userCouponId={}",
                dto.getUserId(), dto.getCouponId(), userCoupon.getId());
    }

    // ==================== 用户端 ====================

    @Override
    public List<UserCouponVO> myCoupons(Integer status) {
        Long userId = SecurityUtil.currentUserId();

        // 先更新过期券状态
        updateExpiredCoupons(userId);

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getCreateTime);
        if (status != null) {
            wrapper.eq(UserCoupon::getStatus, status);
        }

        List<UserCoupon> userCoupons = userCouponMapper.selectList(wrapper);
        return toUserCouponVOList(userCoupons);
    }

    @Override
    public List<UserCouponVO> listAvailable(BigDecimal orderAmount, List<Long> categoryIds) {
        Long userId = SecurityUtil.currentUserId();

        // 更新过期券
        updateExpiredCoupons(userId);

        // 查所有未使用的有效券
        List<UserCoupon> userCoupons = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0)
                        .gt(UserCoupon::getExpireTime, LocalDateTime.now())
        );

        if (userCoupons.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查关联的券模板
        Set<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toSet());
        Map<Long, Coupon> couponMap = couponMapper.selectList(
                        new LambdaQueryWrapper<Coupon>().in(Coupon::getId, couponIds)
                ).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));

        // 过滤 + 计算优惠金额
        List<UserCouponVO> result = new ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            Coupon coupon = couponMap.get(uc.getCouponId());
            if (coupon == null || coupon.getStatus() != 1) {
                continue;
            }

            // 门槛校验（注意：此处基于全单金额，品类券的实际门槛以 place() 中算出的适用金额为准）
            if (orderAmount.compareTo(coupon.getThreshold()) < 0) {
                continue;
            }

            // 范围校验
            if (coupon.getScopeType() == 1 && coupon.getScopeId() != null) {
                if (categoryIds == null || !categoryIds.contains(coupon.getScopeId())) {
                    continue;
                }
            }

            // 计算预估优惠金额（品类券/折扣券基于全单金额估算，实际以 place() 中适用金额为准）
            BigDecimal discountAmount = calculateDiscount(orderAmount, coupon);

            result.add(toUserCouponVO(uc, coupon, discountAmount));
        }

        return result;
    }

    // ==================== 领券中心 ====================

    @Override
    public List<CouponVO> listCenter() {
        Long userId = SecurityUtil.currentUserId();

        // 查所有启用状态的券模板
        List<Coupon> allCoupons = couponMapper.selectList(
                new LambdaQueryWrapper<Coupon>()
                        .eq(Coupon::getStatus, 1)
                        .orderByDesc(Coupon::getCreateTime)
        );

        if (allCoupons.isEmpty()) {
            return Collections.emptyList();
        }

        // 查用户已领过的券模板ID
        Set<Long> claimedCouponIds = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
        ).stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toSet());

        // 排除已领过的
        return allCoupons.stream()
                .filter(c -> !claimedCouponIds.contains(c.getId()))
                .map(this::toCouponVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long couponId) {
        Long userId = SecurityUtil.currentUserId();

        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(70001, "优惠券不存在");
        }
        if (coupon.getStatus() != 1) {
            throw new BusinessException(70002, "该优惠券已下架");
        }

        // 校验未领过
        boolean alreadyClaimed = userCouponMapper.exists(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId)
        );
        if (alreadyClaimed) {
            throw new BusinessException(70008, "您已领取过该优惠券");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        userCoupon.setExpireTime(LocalDateTime.now().plusDays(coupon.getValidDays()));

        userCouponMapper.insert(userCoupon);
        log.info("用户领取优惠券: userId={} couponId={} userCouponId={}",
                userId, couponId, userCoupon.getId());
    }

    // ==================== 内部方法 ====================

    /** 校验券类型与优惠值的合理性 */
    private void validateCouponType(Integer type, BigDecimal discountValue) {
        if (type == 2) {
            // 折扣券：0 < value < 1
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0
                    || discountValue.compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessException(70003, "折扣券的折扣率必须在 0~1 之间（如 0.8 表示8折）");
            }
        } else if (type == 1 || type == 3 || type == 4) {
            // 满减/新人/品类券：value > 0
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(70003, "优惠金额必须大于 0");
            }
        } else {
            throw new BusinessException(70003, "无效的券类型");
        }
    }

    /** 计算优惠金额 */
    static BigDecimal calculateDiscount(BigDecimal orderAmount, Coupon coupon) {
        if (coupon.getType() == 2) {
            // 折扣券：orderAmount × (1 - discountValue)
            return orderAmount.multiply(
                    BigDecimal.ONE.subtract(coupon.getDiscountValue())
            ).setScale(2, RoundingMode.DOWN);
        } else {
            // 满减/新人/品类券：固定减
            BigDecimal discount = coupon.getDiscountValue();
            // 不能超过订单金额
            return discount.compareTo(orderAmount) > 0 ? orderAmount : discount;
        }
    }

    /** 更新当前用户已过期但状态仍为"未使用"的券 */
    private void updateExpiredCoupons(Long userId) {
        userCouponMapper.update(
                new LambdaUpdateWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0)
                        .lt(UserCoupon::getExpireTime, LocalDateTime.now())
                        .set(UserCoupon::getStatus, 2)
        );
    }

    /** Coupon Entity → CouponVO */
    private CouponVO toCouponVO(Coupon entity) {
        String typeText = typeText(entity.getType());

        return CouponVO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .typeText(typeText)
                .threshold(entity.getThreshold())
                .discountValue(entity.getDiscountValue())
                .scopeType(entity.getScopeType())
                .scopeId(entity.getScopeId())
                .validDays(entity.getValidDays())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }

    /** 批量 UserCoupon → UserCouponVO */
    private List<UserCouponVO> toUserCouponVOList(List<UserCoupon> userCoupons) {
        if (userCoupons.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toSet());
        Map<Long, Coupon> couponMap = couponMapper.selectList(
                        new LambdaQueryWrapper<Coupon>().in(Coupon::getId, couponIds)
                ).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));

        return userCoupons.stream()
                .map(uc -> {
                    Coupon coupon = couponMap.get(uc.getCouponId());
                    return toUserCouponVO(uc, coupon, null);
                })
                .toList();
    }

    /** UserCoupon → UserCouponVO */
    private UserCouponVO toUserCouponVO(UserCoupon uc, Coupon coupon, BigDecimal discountAmount) {
        String couponName = coupon != null ? coupon.getName() : "未知券";
        Integer type = coupon != null ? coupon.getType() : 0;
        Integer scopeType = coupon != null ? coupon.getScopeType() : 0;

        String statusText = switch (uc.getStatus()) {
            case 0 -> "未使用";
            case 1 -> "已使用";
            case 2 -> "已过期";
            default -> "未知";
        };

        return UserCouponVO.builder()
                .id(uc.getId())
                .couponId(uc.getCouponId())
                .couponName(couponName)
                .type(type)
                .typeText(typeText(type))
                .scopeType(scopeType)
                .scopeId(coupon != null ? coupon.getScopeId() : null)
                .threshold(coupon != null ? coupon.getThreshold() : BigDecimal.ZERO)
                .discountValue(coupon != null ? coupon.getDiscountValue() : BigDecimal.ZERO)
                .discountAmount(discountAmount)
                .status(uc.getStatus())
                .statusText(statusText)
                .expireTime(uc.getExpireTime())
                .createTime(uc.getCreateTime())
                .build();
    }

    /** 券类型 → 中文 */
    private static String typeText(Integer type) {
        return switch (type) {
            case 1 -> "满减券";
            case 2 -> "折扣券";
            case 3 -> "新人券";
            case 4 -> "品类券";
            default -> "未知";
        };
    }
}
