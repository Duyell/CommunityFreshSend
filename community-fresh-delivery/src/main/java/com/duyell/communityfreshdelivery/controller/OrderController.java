package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.annotation.RateLimit;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.OrderCreateDTO;
import com.duyell.communityfreshdelivery.dto.OrderVO;
import com.duyell.communityfreshdelivery.dto.SortItemDTO;

import java.util.List;
import com.duyell.communityfreshdelivery.service.OrderService;
import com.duyell.communityfreshdelivery.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>订单接口</h2>
 *
 * <p>全部需认证，用户只能操作自己的订单.</p>
 *
 * @author duyell
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Tag(name = "订单", description = "下单/支付/取消/查单/商家接单分拣")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "提交订单")
    @RateLimit(key = "order:place", limit = 5, window = 60, message = "下单过于频繁，请60秒后再试")
    public Result<OrderVO> place(@Valid @RequestBody OrderCreateDTO dto) {
        OrderVO vo = orderService.place(dto);
        return Result.ok("下单成功", vo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public Result<OrderVO> getById(@PathVariable Long id) {
        OrderVO vo = orderService.getById(id);
        return Result.ok(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "我的订单分页")
    public Result<Page<OrderVO>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) Integer status) {
        Page<OrderVO> result = orderService.page(page, size, status);
        return Result.ok(result);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public Result<Void> cancel(@PathVariable Long id,
                                @RequestParam(required = false) String reason) {
        orderService.cancel(id, reason);
        return Result.ok("订单已取消", null);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "模拟支付")
    public Result<Void> pay(@PathVariable Long id) {
        paymentService.pay(id);
        return Result.ok("支付成功", null);
    }

    @PostMapping("/{id}/cancel-pay")
    @Operation(summary = "取消支付")
    public Result<Void> cancelPay(@PathVariable Long id) {
        paymentService.cancelPay(id);
        return Result.ok("已取消支付", null);
    }

    @PutMapping("/{id}/accept")
    @Operation(summary = "商家接单")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.accept(id);
        return Result.ok("已接单", null);
    }

    @PutMapping("/{id}/sort-complete")
    @Operation(summary = "商家分拣完成（含称重实重+缺货标记）")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<Void> sortComplete(@PathVariable Long id,
                                      @RequestBody(required = false) List<SortItemDTO> items) {
        orderService.sortComplete(id, items);
        return Result.ok("分拣完成", null);
    }
}
