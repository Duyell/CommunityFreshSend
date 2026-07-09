package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.ProductBatchSaveDTO;
import com.duyell.communityfreshdelivery.dto.ProductBatchVO;
import com.duyell.communityfreshdelivery.service.ProductBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <h2>批次库存接口</h2>
 *
 * <p>商家端：批次入库 + 查看批次列表 + 临期预警 + FIFO 分配.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Tag(name = "批次库存", description = "批次入库与FIFO管理")
public class ProductBatchController {

    private final ProductBatchService productBatchService;

    @PostMapping
    @Operation(summary = "批次入库")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<ProductBatchVO> create(@Valid @RequestBody ProductBatchSaveDTO dto) {
        ProductBatchVO vo = productBatchService.create(dto);
        return Result.ok("入库成功", vo);
    }

    @GetMapping("/page")
    @Operation(summary = "按商品查看批次列表")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<Page<ProductBatchVO>> pageByProduct(@RequestParam Long productId,
                                                       @RequestParam(required = false) Integer nearExpiry,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        Page<ProductBatchVO> result = productBatchService.pageByProduct(productId, nearExpiry, page, size);
        return Result.ok(result);
    }

    @GetMapping("/near-expiry")
    @Operation(summary = "临期商品列表")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<List<ProductBatchVO>> getNearExpiry() {
        List<ProductBatchVO> list = productBatchService.getNearExpiry();
        return Result.ok(list);
    }

    @GetMapping("/allocate-fifo")
    @Operation(summary = "FIFO 分配库存（分拣指导）")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<List<Map<String, Object>>> allocateFIFO(@RequestParam Long productId,
                                                           @RequestParam int needQuantity) {
        List<Map<String, Object>> result = productBatchService.allocateFIFO(productId, needQuantity);
        return Result.ok(result);
    }
}
