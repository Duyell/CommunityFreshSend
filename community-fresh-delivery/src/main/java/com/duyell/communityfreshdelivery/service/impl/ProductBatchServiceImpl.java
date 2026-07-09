package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.dto.ProductBatchSaveDTO;
import com.duyell.communityfreshdelivery.dto.ProductBatchVO;
import com.duyell.communityfreshdelivery.entity.Product;
import com.duyell.communityfreshdelivery.entity.ProductBatch;
import com.duyell.communityfreshdelivery.mapper.ProductBatchMapper;
import com.duyell.communityfreshdelivery.mapper.ProductMapper;
import com.duyell.communityfreshdelivery.service.ProductBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>批次库存服务实现</h2>
 *
 * <h3>FIFO 分配规则</h3>
 * <ol>
 *   <li>查该商品所有 remaining > 0 的批次</li>
 *   <li>按 expiry_date ASC 排序（早过期的先出，null 排最后）</li>
 *   <li>从前到后扣减 remaining，直到满足 needQuantity 为止</li>
 * </ol>
 *
 * @author duyell
 * @since 2026-06-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductBatchServiceImpl implements ProductBatchService {

    private final ProductBatchMapper productBatchMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductBatchVO create(ProductBatchSaveDTO dto) {
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException(10001, "商品不存在");
        }

        ProductBatch batch = new ProductBatch();
        batch.setProductId(dto.getProductId());
        batch.setBatchNo(dto.getBatchNo());
        batch.setCostPrice(dto.getCostPrice());
        batch.setQuantity(dto.getQuantity());
        batch.setRemaining(dto.getQuantity());
        batch.setProductionDate(dto.getProductionDate());
        batch.setExpiryDate(dto.getExpiryDate());
        batch.setNearExpiry(0);

        productBatchMapper.insert(batch);

        log.info("批次入库: productId={} batchNo={} quantity={}",
                dto.getProductId(), dto.getBatchNo(), dto.getQuantity());

        return toVO(batch, product.getName());
    }

    @Override
    public Page<ProductBatchVO> pageByProduct(Long productId, Integer nearExpiry, int page, int size) {
        LambdaQueryWrapper<ProductBatch> wrapper = new LambdaQueryWrapper<ProductBatch>()
                .eq(ProductBatch::getProductId, productId)
                .orderByAsc(ProductBatch::getExpiryDate);

        if (nearExpiry != null) {
            wrapper.eq(ProductBatch::getNearExpiry, nearExpiry);
        }

        Page<ProductBatch> result = productBatchMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量加载商品名称
        Map<Long, String> productNameMap = loadProductNames(result.getRecords());

        List<ProductBatchVO> vos = result.getRecords().stream()
                .map(b -> toVO(b, productNameMap.get(b.getProductId())))
                .toList();

        Page<ProductBatchVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public List<ProductBatchVO> getNearExpiry() {
        List<ProductBatch> batches = productBatchMapper.selectList(
                new LambdaQueryWrapper<ProductBatch>()
                        .eq(ProductBatch::getNearExpiry, 1)
                        .gt(ProductBatch::getRemaining, 0)
                        .orderByAsc(ProductBatch::getExpiryDate)
        );

        if (batches.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> productNameMap = loadProductNames(batches);

        return batches.stream()
                .map(b -> toVO(b, productNameMap.get(b.getProductId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> allocateFIFO(Long productId, int needQuantity) {
        List<Map<String, Object>> result = new ArrayList<>();
        int remainingNeed = needQuantity;

        // 最多尝试 3 轮：并发冲突时重新加载批次列表，避免因过时数据导致误判"库存不足"
        for (int round = 0; round < 3 && remainingNeed > 0; round++) {
            // 查该商品所有有剩余库存的批次，按过期日升序
            List<ProductBatch> batches = productBatchMapper.selectList(
                    new LambdaQueryWrapper<ProductBatch>()
                            .eq(ProductBatch::getProductId, productId)
                            .gt(ProductBatch::getRemaining, 0)
                            .orderByAsc(ProductBatch::getExpiryDate)
            );

            if (batches.isEmpty()) {
                break;
            }

            for (ProductBatch batch : batches) {
                if (remainingNeed <= 0) {
                    break;
                }

                int allocated = Math.min(batch.getRemaining(), remainingNeed);

                // 原子扣减：WHERE remaining >= allocated，返回 affected rows
                int affected = productBatchMapper.deductRemaining(batch.getId(), allocated);
                if (affected == 0) {
                    // 并发冲突：读到的 remaining 已过期，跳过该批次（下一轮会重新加载）
                    continue;
                }

                Map<String, Object> alloc = new LinkedHashMap<>();
                alloc.put("batchId", batch.getId());
                alloc.put("batchNo", batch.getBatchNo());
                alloc.put("allocatedQuantity", allocated);
                alloc.put("expiryDate", batch.getExpiryDate());
                result.add(alloc);

                remainingNeed -= allocated;
            }
        }

        if (remainingNeed > 0) {
            throw new BusinessException(20004, "商品库存不足");
        }

        log.info("FIFO 分配完成: productId={} needed={} allocated={}",
                productId, needQuantity, needQuantity - remainingNeed);
        return result;
    }

    // ==================== 内部方法 ====================

    /** 批量加载商品名称 */
    private Map<Long, String> loadProductNames(List<ProductBatch> batches) {
        Set<Long> productIds = batches.stream()
                .map(ProductBatch::getProductId)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productMapper.selectList(
                        new LambdaQueryWrapper<Product>().in(Product::getId, productIds)
                ).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
    }

    /** Entity → VO */
    private ProductBatchVO toVO(ProductBatch batch, String productName) {
        return ProductBatchVO.builder()
                .id(batch.getId())
                .productId(batch.getProductId())
                .productName(productName != null ? productName : "")
                .batchNo(batch.getBatchNo())
                .costPrice(batch.getCostPrice())
                .quantity(batch.getQuantity())
                .remaining(batch.getRemaining())
                .productionDate(batch.getProductionDate())
                .expiryDate(batch.getExpiryDate())
                .nearExpiry(batch.getNearExpiry())
                .createTime(batch.getCreateTime())
                .build();
    }
}
