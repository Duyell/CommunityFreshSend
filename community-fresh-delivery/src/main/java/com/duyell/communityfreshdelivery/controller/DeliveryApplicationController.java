package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.DeliveryApplyDTO;
import com.duyell.communityfreshdelivery.dto.DeliveryApplicationVO;
import com.duyell.communityfreshdelivery.dto.DeliveryReviewDTO;
import com.duyell.communityfreshdelivery.service.DeliveryApplicationService;
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
 * <h2>配送员申请接口</h2>
 *
 * <p>用户端：提交申请/查看状态；管理员端：审核.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@RestController
@RequestMapping("/api/delivery-application")
@RequiredArgsConstructor
@Tag(name = "配送员申请", description = "配送员申请与审核")
public class DeliveryApplicationController {

    private final DeliveryApplicationService deliveryApplicationService;

    @PostMapping
    @Operation(summary = "提交配送员申请")
    @PreAuthorize("hasRole('USER')")
    public Result<DeliveryApplicationVO> apply(@Valid @RequestBody DeliveryApplyDTO dto) {
        DeliveryApplicationVO vo = deliveryApplicationService.apply(dto);
        return Result.ok("申请已提交", vo);
    }

    @GetMapping("/my")
    @Operation(summary = "我的申请状态")
    @PreAuthorize("hasRole('USER')")
    public Result<DeliveryApplicationVO> myApplication() {
        DeliveryApplicationVO vo = deliveryApplicationService.myApplication();
        return Result.ok(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "申请列表（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<DeliveryApplicationVO>> page(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) Integer status) {
        Page<DeliveryApplicationVO> result = deliveryApplicationService.pageApplications(page, size, status);
        return Result.ok(result);
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "审核申请（管理员）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DeliveryApplicationVO> review(@PathVariable Long id,
                                                 @Valid @RequestBody DeliveryReviewDTO dto) {
        DeliveryApplicationVO vo = deliveryApplicationService.review(id, dto);
        return Result.ok(vo);
    }
}
