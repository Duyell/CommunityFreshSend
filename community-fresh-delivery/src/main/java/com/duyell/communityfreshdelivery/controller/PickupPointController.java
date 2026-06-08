package com.duyell.communityfreshdelivery.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.duyell.communityfreshdelivery.common.result.Result;
import com.duyell.communityfreshdelivery.entity.PickupPoint;
import com.duyell.communityfreshdelivery.mapper.PickupPointMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h2>自提点接口</h2>
 *
 * <p>公开接口，供用户下单时选择自提点.</p>
 *
 * @author duyell
 * @since 2026-06-08
 */
@RestController
@RequestMapping("/api/pickup-point")
@RequiredArgsConstructor
@Tag(name = "自提点", description = "自提点列表/详情")
public class PickupPointController {

    private final PickupPointMapper pickupPointMapper;

    @GetMapping("/list")
    @Operation(summary = "获取所有营业中自提点")
    public Result<List<PickupPoint>> list() {
        List<PickupPoint> points = pickupPointMapper.selectList(
                new LambdaQueryWrapper<PickupPoint>()
                        .eq(PickupPoint::getStatus, 1)
                        .orderByAsc(PickupPoint::getCreateTime)
        );
        return Result.ok(points);
    }

    @GetMapping("/{id}")
    @Operation(summary = "自提点详情")
    public Result<PickupPoint> getById(@PathVariable Long id) {
        PickupPoint point = pickupPointMapper.selectById(id);
        if (point == null || point.getStatus() != 1) {
            return Result.fail("自提点不存在或已停用");
        }
        return Result.ok(point);
    }
}
