package com.duyell.communityfreshdelivery.controller;

import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.FavoriteVO;
import com.duyell.communityfreshdelivery.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <h2>收藏夹接口</h2>
 *
 * <p>收藏/取消收藏/列表/检查是否已收藏.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
@Tag(name = "收藏夹", description = "商品收藏管理")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{productId}")
    @Operation(summary = "收藏商品")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> add(@PathVariable Long productId) {
        favoriteService.add(productId);
        return Result.ok();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "取消收藏")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> remove(@PathVariable Long productId) {
        favoriteService.remove(productId);
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "我的收藏列表")
    @PreAuthorize("hasRole('USER')")
    public Result<List<FavoriteVO>> list() {
        List<FavoriteVO> list = favoriteService.list();
        return Result.ok(list);
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "是否已收藏")
    @PreAuthorize("hasRole('USER')")
    public Result<Map<String, Boolean>> check(@PathVariable Long productId) {
        boolean favorited = favoriteService.isFavorited(productId);
        return Result.ok(Map.of("favorited", favorited));
    }
}
