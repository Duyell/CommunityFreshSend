package com.duyell.communityfreshdelivery.controller;

import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplyDTO;
import com.duyell.communityfreshdelivery.dto.GroupLeaderApplicationVO;
import com.duyell.communityfreshdelivery.dto.PickupCodeDTO;
import com.duyell.communityfreshdelivery.entity.PickupPoint;
import com.duyell.communityfreshdelivery.service.GroupLeaderApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>团长接口</h2>
 *
 * <p>用户端：提交申请 / 查看申请状态；
 * 团长端：提货码核销 / 查看自提点.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@RestController
@RequestMapping("/api/group-leader")
@RequiredArgsConstructor
@Tag(name = "团长", description = "团长申请/核销")
public class GroupLeaderApplicationController {

    private final GroupLeaderApplicationService applicationService;

    // ==================== 用户端 ====================

    @PostMapping("/apply")
    @Operation(summary = "提交团长申请")
    @PreAuthorize("hasRole('USER')")
    public Result<GroupLeaderApplicationVO> apply(@Valid @RequestBody GroupLeaderApplyDTO dto) {
        GroupLeaderApplicationVO vo = applicationService.apply(dto);
        return Result.ok("申请已提交，请等待审核", vo);
    }

    @GetMapping("/my")
    @Operation(summary = "查看我的团长申请状态")
    @PreAuthorize("hasRole('USER')")
    public Result<GroupLeaderApplicationVO> myApplication() {
        GroupLeaderApplicationVO vo = applicationService.myApplication();
        return Result.ok(vo);
    }

    // ==================== 团长端 ====================

    @GetMapping("/my-pickup-point")
    @Operation(summary = "团长查看自己的自提点")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public Result<List<PickupPoint>> myPickupPoint() {
        List<PickupPoint> points = applicationService.myPickupPoint();
        return Result.ok(points);
    }

    @PostMapping("/verify")
    @Operation(summary = "提货码核销")
    @PreAuthorize("hasRole('GROUP_LEADER')")
    public Result<Void> verifyPickup(@Valid @RequestBody PickupCodeDTO dto) {
        applicationService.verifyPickup(dto.getPickupCode());
        return Result.ok("核销成功", null);
    }
}
