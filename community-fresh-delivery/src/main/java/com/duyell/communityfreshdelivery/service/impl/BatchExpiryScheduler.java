package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.duyell.communityfreshdelivery.entity.ProductBatch;
import com.duyell.communityfreshdelivery.mapper.ProductBatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>批次临期 & 低库存定时预警</h2>
 *
 * <h3>执行规则</h3>
 * <ul>
 *   <li>每天凌晨 2:00 执行</li>
 *   <li>临期标记：remaining > 0 且距过期 ≤ 3 天 → 批量 UPDATE near_expiry=1</li>
 *   <li>低库存预警：stock < stock_threshold → 日志告警</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchExpiryScheduler {

    private final ProductBatchMapper productBatchMapper;

    /** 每天凌晨 2:00 执行 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkNearExpiryAndLowStock() {
        log.info("开始扫描临期批次与低库存...");

        // 1. 批量标记临期批次（remaining > 0 AND expiry_date <= now+3天 AND near_expiry=0）
        LocalDate nearExpiryThreshold = LocalDate.now().plusDays(3);
        int nearCount = productBatchMapper.update(
                new LambdaUpdateWrapper<ProductBatch>()
                        .gt(ProductBatch::getRemaining, 0)
                        .le(ProductBatch::getExpiryDate, nearExpiryThreshold)
                        .eq(ProductBatch::getNearExpiry, 0)
                        .set(ProductBatch::getNearExpiry, 1)
        );
        log.info("临期批次标记完成: {} 个", nearCount);

        // 2. 批量标记已过期批次（remaining > 0 AND expiry_date < now AND near_expiry != 1）
        int expiredCount = productBatchMapper.update(
                new LambdaUpdateWrapper<ProductBatch>()
                        .gt(ProductBatch::getRemaining, 0)
                        .lt(ProductBatch::getExpiryDate, LocalDate.now())
                        .ne(ProductBatch::getNearExpiry, 1)
                        .set(ProductBatch::getNearExpiry, 1)
        );
        if (expiredCount > 0) {
            log.warn("已过期批次标记完成: {} 个", expiredCount);
        }

        // 3. 低库存预警：按 product 聚合批次库存，低于 10 则告警
        List<ProductBatch> allBatches = productBatchMapper.selectList(
                new LambdaQueryWrapper<ProductBatch>()
                        .gt(ProductBatch::getRemaining, 0)
        );
        Map<Long, Integer> stockMap = new java.util.HashMap<>();
        for (ProductBatch b : allBatches) {
            stockMap.merge(b.getProductId(), b.getRemaining(), Integer::sum);
        }
        int lowStockCount = 0;
        for (Map.Entry<Long, Integer> entry : stockMap.entrySet()) {
            if (entry.getValue() < 10) {
                log.warn("库存预警: productId={} totalStock={}", entry.getKey(), entry.getValue());
                lowStockCount++;
            }
        }

        log.info("扫描完成: 临期 {} 个, 已过期 {} 个, 低库存 {} 个",
                nearCount, expiredCount, lowStockCount);
    }
}
