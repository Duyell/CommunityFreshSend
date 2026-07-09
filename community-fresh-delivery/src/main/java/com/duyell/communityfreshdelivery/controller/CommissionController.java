package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.CommissionDetailVO;
import com.duyell.communityfreshdelivery.dto.CommissionVO;
import com.duyell.communityfreshdelivery.service.CommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>团长佣金接口</h2>
 *
 * <p>团长查看收益 + 提现.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@RestController
@RequestMapping("/api/commission")
@RequiredArgsConstructor
@Tag(name = "团长佣金", description = "团长收益与提现")
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping("/summary")
    @Operation(summary = "收益概览")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public Result<CommissionVO> summary() {
        CommissionVO vo = commissionService.summary();
        return Result.ok(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "佣金明细")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public Result<Page<CommissionDetailVO>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) Integer status) {
        Page<CommissionDetailVO> result = commissionService.page(page, size, status);
        return Result.ok(result);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "一键提现")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public Result<Void> withdraw() {
        commissionService.withdraw();
        return Result.ok();
    }
}
