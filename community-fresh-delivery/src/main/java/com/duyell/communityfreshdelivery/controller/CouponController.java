package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.CouponIssueDTO;
import com.duyell.communityfreshdelivery.dto.CouponSaveDTO;
import com.duyell.communityfreshdelivery.dto.CouponVO;
import com.duyell.communityfreshdelivery.dto.UserCouponVO;
import com.duyell.communityfreshdelivery.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * <h2>优惠券接口</h2>
 *
 * <p>管理员端：模板 CRUD + 发券；用户端：查券 + 下单选券.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
@Tag(name = "优惠券", description = "优惠券模板管理与领用")
public class CouponController {

    private final CouponService couponService;

    // ==================== 管理员端 ====================

    @PostMapping
    @Operation(summary = "创建优惠券模板")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CouponVO> create(@Valid @RequestBody CouponSaveDTO dto) {
        CouponVO vo = couponService.create(dto);
        return Result.ok("创建成功", vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑优惠券模板")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CouponVO> update(@PathVariable Long id, @Valid @RequestBody CouponSaveDTO dto) {
        CouponVO vo = couponService.update(id, dto);
        return Result.ok("编辑成功", vo);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/停用模板")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除券模板")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "券模板分页列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<CouponVO>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        Page<CouponVO> result = couponService.page(page, size);
        return Result.ok(result);
    }

    @PostMapping("/issue")
    @Operation(summary = "给用户发券")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> issue(@Valid @RequestBody CouponIssueDTO dto) {
        couponService.issue(dto);
        return Result.ok();
    }

    // ==================== 用户端 ====================

    @GetMapping("/my")
    @Operation(summary = "我的优惠券列表")
    @PreAuthorize("hasRole('USER')")
    public Result<List<UserCouponVO>> myCoupons(@RequestParam(required = false) Integer status) {
        List<UserCouponVO> list = couponService.myCoupons(status);
        return Result.ok(list);
    }

    @GetMapping("/available")
    @Operation(summary = "下单时获取可用优惠券")
    @PreAuthorize("hasRole('USER')")
    public Result<List<UserCouponVO>> listAvailable(@RequestParam BigDecimal orderAmount,
                                                     @RequestParam(required = false) List<Long> categoryIds) {
        List<UserCouponVO> list = couponService.listAvailable(orderAmount, categoryIds);
        return Result.ok(list);
    }

    // ==================== 领券中心 ====================

    @GetMapping("/center")
    @Operation(summary = "领券中心（可领取的券模板）")
    @PreAuthorize("hasRole('USER')")
    public Result<List<CouponVO>> listCenter() {
        List<CouponVO> list = couponService.listCenter();
        return Result.ok(list);
    }

    @PostMapping("/{id}/claim")
    @Operation(summary = "领取优惠券")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> claim(@PathVariable Long id) {
        couponService.claim(id);
        return Result.ok();
    }
}
