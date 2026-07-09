package com.duyell.communityfreshdelivery.controller;

import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * <h2>系统配置管理接口</h2>
 *
 * <p>管理员查看/修改系统配置，修改后自动刷新缓存.</p>
 *
 * @author duyell
 * @since 2026-06-09
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理（管理员）")
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @GetMapping
    @Operation(summary = "配置列表")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> list() {
        Map<String, String> configs = sysConfigService.listAll();
        return Result.ok(configs);
    }

    @PutMapping("/{key}")
    @Operation(summary = "修改配置")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable String key, @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null || value.isBlank()) {
            return Result.fail("配置值不能为空");
        }
        sysConfigService.update(key, value);
        return Result.ok();
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新缓存")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refresh() {
        sysConfigService.refreshCache();
        return Result.ok();
    }
}
