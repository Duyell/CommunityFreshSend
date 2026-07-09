package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplicationVO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderReviewDTO;
import com.duyell.communityfreshdelivery.service.GroupLeaderApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h2>管理员团长审核接口</h2>
 *
 * <p>管理端：分页查看团长申请 / 审核团长申请.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@RestController
@RequestMapping("/api/admin/group-leader")
@RequiredArgsConstructor
@Tag(name = "团长审核", description = "管理员审核团长申请")
public class AdminGroupLeaderController {

    private final GroupLeaderApplicationService applicationService;

    @GetMapping("/page")
    @Operation(summary = "管理员分页查看团长申请")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<GroupLeaderApplicationVO>> pageApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Page<GroupLeaderApplicationVO> result = applicationService.pageApplications(page, size, status);
        return Result.ok(result);
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "管理员审核团长申请")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<GroupLeaderApplicationVO> review(@PathVariable Long id,
                                                   @Valid @RequestBody GroupLeaderReviewDTO dto) {
        GroupLeaderApplicationVO vo = applicationService.review(id, dto);
        String msg = Boolean.TRUE.equals(dto.getApproved()) ? "审核通过" : "已拒绝";
        return Result.ok(msg, vo);
    }
}
