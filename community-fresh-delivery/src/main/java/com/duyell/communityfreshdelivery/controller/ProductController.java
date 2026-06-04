package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.ProductSaveDTO;
import com.duyell.communityfreshdelivery.dto.ProductVO;
import com.duyell.communityfreshdelivery.service.ProductService;
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

/**
 * <h2>商品接口</h2>
 *
 * <p>商家管理接口需 {@code ROLE_MERCHANT} 角色，用户浏览接口公开.</p>
 *
 * @author duyell
 * @since 2026-06-04
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品 CRUD + 列表查询")
public class ProductController {

    private final ProductService productService;

    /**
     * 创建商品（含 SKU 列表）.
     */
    @PostMapping
    @Operation(summary = "创建商品")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<ProductVO> create(@Valid @RequestBody ProductSaveDTO dto) {
        ProductVO vo = productService.create(dto);
        return Result.ok("创建成功", vo);
    }

    /**
     * 编辑商品（SKU 先删后插）.
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑商品")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<ProductVO> update(@PathVariable Long id,
                                    @Valid @RequestBody ProductSaveDTO dto) {
        ProductVO vo = productService.update(id, dto);
        return Result.ok("编辑成功", vo);
    }

    /**
     * 更新商品状态（上架/下架/售罄）.
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新商品状态")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam Integer status) {
        productService.updateStatus(id, status);
        return Result.ok("状态已更新", null);
    }

    /**
     * 删除商品（逻辑删除）.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok("已删除", null);
    }

    /**
     * 商品详情（公开）.
     */
    @GetMapping("/{id}")
    @Operation(summary = "商品详情")
    public Result<ProductVO> getById(@PathVariable Long id) {
        ProductVO vo = productService.getById(id);
        if (vo == null) {
            return Result.fail(10001, "商品不存在");
        }
        return Result.ok(vo);
    }

    /**
     * 商家端分页列表.
     */
    @GetMapping("/page")
    @Operation(summary = "商品分页列表")
    @PreAuthorize("hasRole('MERCHANT')")
    public Result<Page<ProductVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        Page<ProductVO> result = productService.page(page, size, categoryId, keyword);
        return Result.ok(result);
    }
}
