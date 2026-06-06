package com.duyell.communityfreshdelivery.controller;

import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.AddressSaveDTO;
import com.duyell.communityfreshdelivery.dto.AddressVO;
import com.duyell.communityfreshdelivery.service.AddressService;
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
 * <h2>收货地址接口</h2>
 *
 * <p>全部接口需认证，地址操作仅限用户本人.</p>
 *
 * @author duyell
 * @since 2026-06-06
 */
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@Tag(name = "收货地址", description = "收货地址 CRUD + 设默认")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "地址列表")
    public Result<List<AddressVO>> list() {
        List<AddressVO> list = addressService.list();
        return Result.ok(list);
    }

    @PostMapping
    @Operation(summary = "新增地址")
    public Result<AddressVO> create(@Valid @RequestBody AddressSaveDTO dto) {
        AddressVO vo = addressService.create(dto);
        return Result.ok("新增成功", vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑地址")
    public Result<AddressVO> update(@PathVariable Long id,
                                    @Valid @RequestBody AddressSaveDTO dto) {
        AddressVO vo = addressService.update(id, dto);
        return Result.ok("编辑成功", vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.ok("已删除", null);
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认地址")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.ok("已设为默认", null);
    }
}
