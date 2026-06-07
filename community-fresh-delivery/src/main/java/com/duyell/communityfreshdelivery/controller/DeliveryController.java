package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.DeliveryVO;
import com.duyell.communityfreshdelivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>配送员接口</h2>
 *
 * <p>全部需 ROLE_DELIVERY 角色，配送员只能操作自己的配送记录.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY')")
@Tag(name = "配送员", description = "接单大厅/抢单/取货/送达")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/hall")
    @Operation(summary = "接单大厅")
    public Result<Page<DeliveryVO>> hall(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Page<DeliveryVO> result = deliveryService.hall(page, size);
        return Result.ok(result);
    }

    @PostMapping("/grab/{orderId}")
    @Operation(summary = "抢单")
    public Result<DeliveryVO> grab(@PathVariable Long orderId) {
        DeliveryVO vo = deliveryService.grab(orderId);
        return Result.ok("抢单成功", vo);
    }

    @PutMapping("/{orderId}/pickup")
    @Operation(summary = "取货确认")
    public Result<Void> pickup(@PathVariable Long orderId) {
        deliveryService.confirmPickup(orderId);
        return Result.ok("取货确认成功", null);
    }

    @PutMapping("/{orderId}/deliver")
    @Operation(summary = "送达确认")
    public Result<Void> deliver(@PathVariable Long orderId) {
        deliveryService.confirmDelivery(orderId);
        return Result.ok("送达确认成功", null);
    }

    @GetMapping("/my")
    @Operation(summary = "我的配送记录")
    public Result<Page<DeliveryVO>> myDeliveries(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) Integer status) {
        Page<DeliveryVO> result = deliveryService.myDeliveries(page, size, status);
        return Result.ok(result);
    }
}
