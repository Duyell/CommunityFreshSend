package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.ReviewCreateDTO;
import com.duyell.communityfreshdelivery.dto.ReviewVO;
import com.duyell.communityfreshdelivery.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>评价接口</h2>
 *
 * <p>用户评价商品（创建）/ 查看商品评价列表 / 我的评价历史.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@Tag(name = "评价", description = "商品评价/评分")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "创建评价")
    @PreAuthorize("hasRole('USER')")
    public Result<ReviewVO> create(@Valid @RequestBody ReviewCreateDTO dto) {
        ReviewVO vo = reviewService.create(dto);
        return Result.ok("评价成功", vo);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查看订单评价")
    public Result<List<ReviewVO>> getByOrderId(@PathVariable Long orderId) {
        List<ReviewVO> vos = reviewService.getByOrderId(orderId);
        return Result.ok(vos);
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "查看商品评价列表（公开）")
    public Result<Page<ReviewVO>> getByProductId(@PathVariable Long productId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Page<ReviewVO> result = reviewService.getByProductId(productId, page, size);
        return Result.ok(result);
    }

    @GetMapping("/my")
    @Operation(summary = "我的评价历史")
    @PreAuthorize("hasRole('USER')")
    public Result<Page<ReviewVO>> myReviews(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        Page<ReviewVO> result = reviewService.myReviews(page, size);
        return Result.ok(result);
    }
}
