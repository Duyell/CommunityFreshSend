package com.duyell.communityfreshdelivery.controller;

import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.CartAddDTO;
import com.duyell.communityfreshdelivery.dto.CartItemVO;
import com.duyell.communityfreshdelivery.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>购物车接口</h2>
 *
 * <p>全部需认证，基于 Redis Hash 实现，一个用户一个购物车.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "购物车", description = "加购/查车/改数量/删单品/清空")
public class CartController {

    private final CartService cartService;

    @PostMapping
    @Operation(summary = "加入购物车")
    public Result<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return Result.ok("已加入购物车", null);
    }

    @PutMapping("/{skuId}")
    @Operation(summary = "修改数量")
    public Result<Void> updateQty(@PathVariable Long skuId,
                                  @Valid @RequestBody CartAddDTO dto) {
        dto.setSkuId(skuId);
        cartService.updateQty(dto);
        return Result.ok("数量已更新", null);
    }

    @DeleteMapping("/{skuId}")
    @Operation(summary = "删除购物车单品")
    public Result<Void> remove(@PathVariable Long skuId) {
        cartService.remove(skuId);
        return Result.ok("已移除", null);
    }

    @DeleteMapping
    @Operation(summary = "清空购物车")
    public Result<Void> clear() {
        cartService.clear();
        return Result.ok("购物车已清空", null);
    }

    @GetMapping
    @Operation(summary = "查看购物车")
    public Result<List<CartItemVO>> list() {
        List<CartItemVO> list = cartService.list();
        return Result.ok(list);
    }
}
