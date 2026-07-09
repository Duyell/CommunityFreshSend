package com.duyell.communityfreshdelivery.controller;

import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.DashboardVO;
import com.duyell.communityfreshdelivery.dto.HotProductVO;
import com.duyell.communityfreshdelivery.dto.SalesStatsVO;
import com.duyell.communityfreshdelivery.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>数据看板接口</h2>
 *
 * <p>商家端首页数据统计.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "数据看板", description = "商家端数据统计")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/today")
    @Operation(summary = "今日概况")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<DashboardVO> today() {
        DashboardVO vo = dashboardService.today();
        return Result.ok(vo);
    }

    @GetMapping("/sales")
    @Operation(summary = "销量统计（按日/周/月）")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<List<SalesStatsVO>> salesStats(@RequestParam(defaultValue = "day") String period) {
        List<SalesStatsVO> list = dashboardService.salesStats(period);
        return Result.ok(list);
    }

    @GetMapping("/hot-products")
    @Operation(summary = "热销 TOP 10")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<List<HotProductVO>> hotProducts() {
        List<HotProductVO> list = dashboardService.hotProducts();
        return Result.ok(list);
    }
}
