package com.duyell.communityfreshdelivery.controller;

import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.CategoryVO;
import com.duyell.communityfreshdelivery.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>商品分类接口</h2>
 *
 * <p>分类浏览是公开接口，由 {@code SecurityConfig} 放行.</p>
 *
 * @author duyell
 * @since 2026-06-04
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Tag(name = "商品分类", description = "分类树查询")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类树，前端可直接渲染为级联菜单或导航栏.
     *
     * @return 一级分类列表，每个节点含 children
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树")
    public Result<List<CategoryVO>> getTree() {
        List<CategoryVO> tree = categoryService.getCategoryTree();
        return Result.ok(tree);
    }
}
