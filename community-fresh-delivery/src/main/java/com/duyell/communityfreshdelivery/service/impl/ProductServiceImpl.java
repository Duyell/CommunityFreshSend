package com.duyell.communityfreshdelivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.duyell.communityfreshdelivery.common.exception.BusinessException;
import com.duyell.communityfreshdelivery.dto.ProductSaveDTO;
import com.duyell.communityfreshdelivery.dto.ProductVO;
import com.duyell.communityfreshdelivery.entity.Product;
import com.duyell.communityfreshdelivery.entity.ProductSku;
import com.duyell.communityfreshdelivery.mapper.ProductMapper;
import com.duyell.communityfreshdelivery.mapper.ProductSkuMapper;
import com.duyell.communityfreshdelivery.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * <h2>商品服务实现</h2>
 *
 * <h3>关键设计</h3>
 * <ul>
 *   <li><b>主子表事务</b> — create/update 操作 product + product_sku 在同一事务</li>
 *   <li><b>SKU 先删后插</b> — 编辑时 deleteByProductId + batch insert，避免逐个比对</li>
 *   <li><b>软删除</b> — delete 走 MyBatis-Plus @TableLogic，只标记 deleted=1</li>
 * </ul>
 *
 * @author duyell
 * @since 2026-06-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    @Override
    @Transactional
    public ProductVO create(ProductSaveDTO dto) {
        // 1. 插入主表
        Product product = new Product();
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setImages(dto.getImages());
        product.setIsWeighted(dto.getIsWeighted() != null ? dto.getIsWeighted() : 0);
        product.setStatus(2); // 新建默认下架，商品主需要在后台手动上架
        productMapper.insert(product);

        // 2. 批量插入 SKU
        List<ProductSku> skus = buildSkuList(product.getId(), dto);
        for (ProductSku sku : skus) {
            productSkuMapper.insert(sku);
        }

        log.info("商品创建成功: id={}, name={}, sku数={}", product.getId(), dto.getName(), skus.size());
        return getById(product.getId());
    }

    @Override
    @Transactional
    public ProductVO update(Long id, ProductSaveDTO dto) {
        // 1. 校验商品存在
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(10001, "商品不存在");
        }

        // 2. 更新主表
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setImages(dto.getImages());
        product.setIsWeighted(dto.getIsWeighted() != null ? dto.getIsWeighted() : 0);
        productMapper.updateById(product);

        // 3. 先删后插 SKU
        productSkuMapper.deleteByProductId(id);
        List<ProductSku> skus = buildSkuList(id, dto);
        for (ProductSku sku : skus) {
            productSkuMapper.insert(sku);
        }

        log.info("商品编辑成功: id={}, name={}, sku数={}", id, dto.getName(), skus.size());
        return getById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(10001, "商品不存在");
        }
        product.setStatus(status);
        productMapper.updateById(product);
        log.info("商品状态更新: id={}, status={}", id, status);
    }

    @Override
    public void delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(10001, "商品不存在");
        }
        productMapper.deleteById(id);
        // 同时逻辑删除关联的 SKU
        productSkuMapper.deleteByProductId(id);
        log.info("商品已删除: id={}, name={}", id, product.getName());
    }

    @Override
    public ProductVO getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }

        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, id)
                        .orderByAsc(ProductSku::getId)
        );

        return toVO(product, skus);
    }

    @Override
    public Page<ProductVO> page(int page, int size, Long categoryId, String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Product::getName, keyword);
        }
        wrapper.orderByDesc(Product::getCreateTime);

        Page<Product> result = productMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为 ProductVO 列表（不含 SKU），计算最低售价
        List<ProductVO> voList = result.getRecords().stream()
                .map(p -> {
                    List<ProductSku> skus = productSkuMapper.selectList(
                            new LambdaQueryWrapper<ProductSku>()
                                    .eq(ProductSku::getProductId, p.getId())
                    );
                    return toVO(p, skus);
                })
                .toList();

        Page<ProductVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 内部方法 ====================

    /** 将 DTO 中的 SKU 列表转为实体列表 */
    private List<ProductSku> buildSkuList(Long productId, ProductSaveDTO dto) {
        return dto.getSkus().stream()
                .map(item -> {
                    ProductSku sku = new ProductSku();
                    sku.setProductId(productId);
                    sku.setSpecName(item.getSpecName());
                    sku.setPrice(item.getPrice());
                    sku.setStock(item.getStock());
                    sku.setStockThreshold(10);
                    sku.setStatus(1);
                    return sku;
                })
                .toList();
    }

    /** Entity → VO */
    private ProductVO toVO(Product product, List<ProductSku> skus) {
        // 最低售价
        var minPrice = skus.stream()
                .map(ProductSku::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);

        List<ProductVO.SkuVO> skuVOs = skus.stream()
                .map(s -> ProductVO.SkuVO.builder()
                        .id(s.getId())
                        .specName(s.getSpecName())
                        .price(s.getPrice())
                        .stock(s.getStock())
                        .build())
                .toList();

        return ProductVO.builder()
                .id(product.getId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .description(product.getDescription())
                .images(product.getImages())
                .status(product.getStatus())
                .isWeighted(product.getIsWeighted())
                .minPrice(minPrice)
                .createTime(product.getCreateTime())
                .skus(skuVOs)
                .build();
    }
}
